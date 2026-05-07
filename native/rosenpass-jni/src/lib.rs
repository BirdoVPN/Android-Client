//! Rosenpass JNI bindings — Birdo Android client.
//!
//! This crate is loaded at runtime as `librosenpass_jni.so` by
//! `app.birdo.vpn.service.RosenpassNative`. It exposes the **minimum** Rust
//! surface needed to perform a real bilateral post-quantum WireGuard PSK
//! exchange against a Birdo VPN node running upstream `rosenpass`.
//!
//! ## Status of this crate
//!
//! This is the **foundation milestone** (M1). The JNI surface, build system,
//! gradle integration, and Kotlin loader are all wired up end-to-end. The
//! actual handshake state machine is intentionally `unimplemented!()` with
//! detailed `TODO(M2)` comments referencing the exact upstream `rosenpass`
//! crate functions to call. See `native/ROADMAP.md` for the full plan.
//!
//! ## Why we don't just `use rosenpass`
//!
//! The upstream `rosenpass` crate's library API is in flux — its
//! `protocol::CryptoServer` state machine is `pub(crate)` in several recent
//! releases. We will vendor the relevant modules under `vendor/rosenpass/`
//! once the protocol body lands in M2. Until then, this crate carries only
//! the JNI surface plus PQ primitive crates from `pqcrypto-*` so the .so
//! artifact builds and loads on-device, allowing the rest of the integration
//! (Kotlin loader, gradle task, CI build matrix) to be exercised in CI.
//!
//! ## Safety
//!
//! Every `extern "system"` function below validates inputs and converts
//! panics to a thrown Java exception via `env.exception_check()` /
//! `env.throw_new()`. Returning a JVM into an unwound Rust stack would be
//! UB; we use `panic = "abort"` in `Cargo.toml` as a belt-and-braces.

#![deny(unsafe_op_in_unsafe_fn)]
#![warn(clippy::pedantic)]
#![allow(clippy::missing_errors_doc, clippy::missing_panics_doc)]

use jni::objects::{JByteArray, JClass, JObject, JString};
use jni::sys::{jint, jobjectArray};
use jni::JNIEnv;
use log::LevelFilter;
use std::sync::Once;
use zeroize::Zeroizing;

mod errors;
mod handshake;

use errors::{throw_runtime, JniErr};

const TAG: &str = "RosenpassJNI";
const PSK_LEN_BYTES: usize = 32;

static INIT_LOGGER: Once = Once::new();

fn init_logger_once() {
    INIT_LOGGER.call_once(|| {
        android_logger::init_once(
            android_logger::Config::default()
                .with_max_level(LevelFilter::Info)
                .with_tag(TAG),
        );
        log::info!("rosenpass-jni initialised (M1 foundation)");
    });
}

// ── nativeVersion ──────────────────────────────────────────────────────────

/// Returns the version of the loaded native library.
///
/// Used by `RosenpassNative.getNativeVersion()` for diagnostics — lets the
/// Kotlin side detect ABI/build mismatches between the .so and the loader.
#[no_mangle]
pub extern "system" fn Java_app_birdo_vpn_service_RosenpassNative_nativeVersion<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
) -> JString<'a> {
    init_logger_once();
    let v = format!(
        "rosenpass-jni {} ({}, {})",
        env!("CARGO_PKG_VERSION"),
        std::env::consts::ARCH,
        if cfg!(debug_assertions) { "debug" } else { "release" }
    );
    env.new_string(v).unwrap_or_else(|_| {
        // If we can't even build a string something is very wrong with the JVM —
        // return a static empty string rather than panicking.
        env.new_string("").expect("JVM cannot allocate empty string")
    })
}

// ── nativeGenerateKeypair ──────────────────────────────────────────────────

/// Generate a long-lived Classic McEliece keypair.
///
/// Returns a 2-element `byte[][]`: `[publicKey, secretKey]`.
///
/// **Note:** Classic McEliece public keys are ~261 KB and secret keys ~6 MB.
/// This is normal — they're allocated in `Zeroizing` buffers so the secret
/// material is wiped on drop. The keypair is generated **once per install**
/// and persisted via `RosenpassManager.persistKeypair()` to avoid the cost
/// on every connect.
#[no_mangle]
pub extern "system" fn Java_app_birdo_vpn_service_RosenpassNative_nativeGenerateKeypair<'a>(
    mut env: JNIEnv<'a>,
    class: JClass<'a>,
) -> jobjectArray {
    init_logger_once();

    match handshake::generate_keypair() {
        Ok(kp) => {
            // Allocate a 2-element byte[][] on the JVM heap and fill it.
            let byte_array_class = match env.find_class("[B") {
                Ok(c) => c,
                Err(e) => {
                    throw_runtime(&mut env, &format!("find_class([B) failed: {e}"));
                    return std::ptr::null_mut();
                }
            };
            let outer = match env.new_object_array(2, byte_array_class, JObject::null()) {
                Ok(arr) => arr,
                Err(e) => {
                    throw_runtime(&mut env, &format!("alloc byte[][] failed: {e}"));
                    return std::ptr::null_mut();
                }
            };

            let pk_jb = match env.byte_array_from_slice(&kp.public_key) {
                Ok(b) => b,
                Err(e) => {
                    throw_runtime(&mut env, &format!("alloc pk bytes failed: {e}"));
                    return std::ptr::null_mut();
                }
            };
            let sk_jb = match env.byte_array_from_slice(kp.secret_key.as_ref()) {
                Ok(b) => b,
                Err(e) => {
                    throw_runtime(&mut env, &format!("alloc sk bytes failed: {e}"));
                    return std::ptr::null_mut();
                }
            };

            if env.set_object_array_element(&outer, 0, pk_jb).is_err()
                || env.set_object_array_element(&outer, 1, sk_jb).is_err()
            {
                throw_runtime(&mut env, "set_object_array_element failed");
                return std::ptr::null_mut();
            }
            let _ = class; // silence unused warning
            outer.into_raw()
        }
        Err(e) => {
            throw_runtime(&mut env, &format!("generate_keypair: {e}"));
            std::ptr::null_mut()
        }
    }
}

// ── nativeInitiateHandshake ────────────────────────────────────────────────

/// Build the first Rosenpass handshake message (`InitHello`) addressed to the
/// peer identified by `peerStaticPublicKey`.
///
/// The returned `byte[]` is the on-the-wire `InitHello` frame that the caller
/// should send to the node's `rosenpass_endpoint` over UDP.
///
/// `clientSecretKey` is the static Classic McEliece secret key produced by
/// `nativeGenerateKeypair`. It is wiped from native memory on drop and is
/// not retained beyond this call.
///
/// **Status:** stub — see ROADMAP.md M2. Returns `null` and logs a warning
/// until the protocol body lands. The Kotlin caller then falls back to the
/// existing server-provided PSK path.
#[no_mangle]
pub extern "system" fn Java_app_birdo_vpn_service_RosenpassNative_nativeInitiateHandshake<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    peer_static_public_key: JByteArray<'a>,
    client_secret_key: JByteArray<'a>,
) -> JByteArray<'a> {
    init_logger_once();

    let peer_pk = match env.convert_byte_array(&peer_static_public_key) {
        Ok(b) => b,
        Err(e) => {
            throw_runtime(&mut env, &format!("read peer_static_public_key: {e}"));
            return JObject::null().into();
        }
    };
    let sk = match env.convert_byte_array(&client_secret_key) {
        Ok(b) => Zeroizing::new(b),
        Err(e) => {
            throw_runtime(&mut env, &format!("read client_secret_key: {e}"));
            return JObject::null().into();
        }
    };

    match handshake::initiate(&peer_pk, &sk) {
        Ok(msg) => env
            .byte_array_from_slice(&msg)
            .unwrap_or_else(|_| JObject::null().into()),
        Err(JniErr::NotImplemented(reason)) => {
            log::warn!(target: TAG, "initiate_handshake: {reason}");
            JObject::null().into()
        }
        Err(e) => {
            throw_runtime(&mut env, &format!("initiate_handshake: {e}"));
            JObject::null().into()
        }
    }
}

// ── nativeHandleResponse ───────────────────────────────────────────────────

/// Process the peer's `RespHello` reply and derive the 32-byte WireGuard PSK.
///
/// `responseMessage` is the raw UDP payload received from the node in reply
/// to the `InitHello` produced by `nativeInitiateHandshake`. Returns the
/// derived 32-byte PSK on success, or `null` if the response is malformed,
/// authentication fails, or the protocol body is not yet implemented.
///
/// **Status:** stub — see ROADMAP.md M2.
#[no_mangle]
pub extern "system" fn Java_app_birdo_vpn_service_RosenpassNative_nativeHandleResponse<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    response_message: JByteArray<'a>,
    client_secret_key: JByteArray<'a>,
) -> JByteArray<'a> {
    init_logger_once();

    let resp = match env.convert_byte_array(&response_message) {
        Ok(b) => b,
        Err(e) => {
            throw_runtime(&mut env, &format!("read response_message: {e}"));
            return JObject::null().into();
        }
    };
    let sk = match env.convert_byte_array(&client_secret_key) {
        Ok(b) => Zeroizing::new(b),
        Err(e) => {
            throw_runtime(&mut env, &format!("read client_secret_key: {e}"));
            return JObject::null().into();
        }
    };

    match handshake::handle_response(&resp, &sk) {
        Ok(psk) => {
            debug_assert_eq!(psk.len(), PSK_LEN_BYTES);
            env.byte_array_from_slice(&psk)
                .unwrap_or_else(|_| JObject::null().into())
        }
        Err(JniErr::NotImplemented(reason)) => {
            log::warn!(target: TAG, "handle_response: {reason}");
            JObject::null().into()
        }
        Err(e) => {
            throw_runtime(&mut env, &format!("handle_response: {e}"));
            JObject::null().into()
        }
    }
}

// ── nativeRekeyInterval ────────────────────────────────────────────────────

/// Returns the recommended re-key interval in seconds.
///
/// Per the Rosenpass spec, peers should re-derive a fresh PSK approximately
/// every 120 s. Exposed as a function so the schedule can be tightened
/// in future without redeploying clients.
#[no_mangle]
pub extern "system" fn Java_app_birdo_vpn_service_RosenpassNative_nativeRekeyIntervalSeconds<'a>(
    _env: JNIEnv<'a>,
    _class: JClass<'a>,
) -> jint {
    handshake::REKEY_INTERVAL_SECONDS
}
