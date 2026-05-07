//! Rosenpass handshake — Classic McEliece (long-lived) + ML-KEM (ephemeral).
//!
//! This module is the *only* place that touches PQ KEM crates and key material.
//! Every secret returned via `Zeroizing<Vec<u8>>` so it's wiped from native
//! memory on drop.
//!
//! ## M1 (this milestone) — what's implemented
//! - `generate_keypair`: real Classic McEliece keypair via `pqcrypto-classicmceliece`.
//! - `REKEY_INTERVAL_SECONDS`: constant from the Rosenpass spec.
//!
//! ## M2 (next milestone) — what's NOT implemented
//! - `initiate`: build `InitHello` frame (Classic McEliece encapsulation,
//!   ephemeral ML-KEM keypair, transcript hash, MAC).
//! - `handle_response`: parse `RespHello` (decapsulate ML-KEM ciphertext,
//!   verify MAC, derive PSK via HKDF over combined transcript).
//!
//! These are stubbed `Err(JniErr::NotImplemented(_))` so the library still
//! loads in production and the Kotlin caller transparently falls back to
//! the existing server-provided PSK path. See `native/ROADMAP.md`.

use crate::errors::JniErr;
use pqcrypto_classicmceliece::mceliece460896 as mceliece;
use pqcrypto_traits::kem::{PublicKey as KemPublicKey, SecretKey as KemSecretKey};
use zeroize::Zeroizing;

/// Per the Rosenpass spec §5: re-key every ~120 s.
pub const REKEY_INTERVAL_SECONDS: i32 = 120;

pub struct StaticKeypair {
    pub public_key: Vec<u8>,
    pub secret_key: Zeroizing<Vec<u8>>,
}

/// Generate a long-lived Classic McEliece 460896 keypair.
///
/// `pqcrypto-classicmceliece` uses the NIST round-4 reference implementation
/// internally, with `getrandom` as the entropy source (mapped to `/dev/urandom`
/// on Android).
pub fn generate_keypair() -> Result<StaticKeypair, JniErr> {
    let (pk, sk) = mceliece::keypair();
    Ok(StaticKeypair {
        public_key: pk.as_bytes().to_vec(),
        secret_key: Zeroizing::new(sk.as_bytes().to_vec()),
    })
}

/// Build the on-wire `InitHello` frame.
///
/// **TODO(M2)** — needs:
/// 1. Generate ephemeral ML-KEM-512 keypair (`pqcrypto-mlkem`).
/// 2. Encapsulate against `peer_static_public_key` (Classic McEliece) →
///    `(ct_static, ss_static)`.
/// 3. Build transcript per Rosenpass spec §4.2 (ProtocolNamePart, peer pid,
///    sender pid, ephemeral_pk, ct_static).
/// 4. Compute MAC over transcript with `chaining_key` derived from `ss_static`.
/// 5. Serialise frame as: `[type=0x81 | sid_i(16) | ct_static(188) | epk_i(800) | mac(16)]`
///    per the rosenpass `protocol::message::InitHello` layout.
///
/// Reference: <https://rosenpass.eu/whitepaper.pdf> §4 + the upstream
/// `rosenpass::protocol::CryptoServer::initiate_conversation` implementation.
pub fn initiate(
    _peer_static_public_key: &[u8],
    _client_secret_key: &Zeroizing<Vec<u8>>,
) -> Result<Vec<u8>, JniErr> {
    Err(JniErr::NotImplemented(
        "initiate(): rosenpass InitHello frame builder pending M2 — \
         see native/ROADMAP.md and rosenpass crate's protocol::CryptoServer",
    ))
}

/// Parse `RespHello` and derive the 32-byte WireGuard PSK.
///
/// **TODO(M2)** — needs:
/// 1. Parse frame: `[type=0x82 | sid_r(16) | sid_i(16) | ct_eph(736) | biscuit(174) | mac(16)]`.
/// 2. Verify MAC against derived chaining key from M2-step-1.
/// 3. Decapsulate `ct_eph` with our ephemeral ML-KEM secret → `ss_eph`.
/// 4. Combine `ss_static + ss_eph` via HKDF-SHA512 with the spec's labels.
/// 5. Output 32 bytes of OKM as the WireGuard PSK.
/// 6. Zeroize all KEM secrets and intermediate state.
pub fn handle_response(
    _response_message: &[u8],
    _client_secret_key: &Zeroizing<Vec<u8>>,
) -> Result<Zeroizing<Vec<u8>>, JniErr> {
    Err(JniErr::NotImplemented(
        "handle_response(): rosenpass RespHello processor pending M2 — \
         see native/ROADMAP.md",
    ))
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Sanity check the KEM is wired correctly. Generates a real keypair
    /// (slow — Classic McEliece is ~1s on a workstation, slower on a phone),
    /// so it's gated to release-with-debug profile only via `--release` in CI.
    #[test]
    #[ignore = "slow — run with `cargo test --release -- --ignored`"]
    fn keypair_roundtrip() {
        let kp = generate_keypair().expect("generate_keypair");

        // Sanity-check the published Classic McEliece 460896 sizes.
        // pk ≈ 524 KB, sk ≈ 13 KB — these are the round-4 NIST reference sizes.
        assert!(kp.public_key.len() > 100_000, "pk size suspicious: {}", kp.public_key.len());
        assert!(kp.secret_key.len() > 10_000, "sk size suspicious: {}", kp.secret_key.len());
    }

    #[test]
    fn initiate_returns_not_implemented() {
        let r = initiate(&[0u8; 32], &Zeroizing::new(vec![0u8; 32]));
        assert!(matches!(r, Err(JniErr::NotImplemented(_))));
    }

    #[test]
    fn handle_response_returns_not_implemented() {
        let r = handle_response(&[0u8; 32], &Zeroizing::new(vec![0u8; 32]));
        assert!(matches!(r, Err(JniErr::NotImplemented(_))));
    }
}
