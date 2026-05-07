# Rosenpass JNI — Roadmap

This document tracks the **honest** state of the post-quantum WireGuard PSK
exchange in the Birdo Android client. It is the source of truth for what
works, what doesn't, and what blocks production rollout.

> **TL;DR — production status as of branch `feat/rosenpass-native-jni`:**
> M1 foundation only. Native lib loads, `generate_keypair` works, the
> handshake protocol body returns `NotImplemented` and the client transparently
> falls back to the existing server-provided PSK path. **Do NOT advertise
> bilateral post-quantum protection yet.**

---

## Milestones

### ✅ M1 — Foundation (this branch)

Everything that needs to exist *around* the protocol implementation, so M2 is
purely additive and doesn't risk shipping half-baked crypto by accident.

- [x] Rust crate `native/rosenpass-jni/` with `cdylib` target
- [x] JNI surface (`#[no_mangle] extern "system" fn`) for all 5 functions
- [x] PQ KEM crate dependencies pinned (`pqcrypto-mlkem`, `pqcrypto-classicmceliece`)
- [x] `generate_keypair` — real Classic McEliece 460896 keypair generation
- [x] Panic-to-exception conversion via `errors::throw_runtime`
- [x] Secret-wiping with `Zeroizing`/`secrecy`
- [x] Android logger init wired to logcat tag `RosenpassJNI`
- [x] Kotlin loader `RosenpassNative` with `UnsatisfiedLinkError` fallback
- [x] PowerShell + Bash build scripts invoking `cargo ndk` for arm64-v8a, armeabi-v7a, x86_64
- [x] CI workflow installs Rust toolchain + cargo-ndk + 3 Android targets
- [x] Gradle task `:app:buildRustLibs` invoked before `mergeReleaseNativeLibs`
- [x] `panic = "abort"` in release profile (no UB across FFI on panic)
- [x] `.gitignore` excludes built artifacts

### ⏳ M2 — Handshake protocol body

The actual Rosenpass exchange. Concretely the work in
[`handshake.rs::initiate`](./rosenpass-jni/src/handshake.rs) and
`handshake.rs::handle_response`.

Two viable approaches — pick one before starting:

#### Option A — Vendor `rosenpass` crate modules (recommended)

- Add `vendor/rosenpass/` as a git submodule pinned to a tagged release
- Replace stub bodies with calls into `rosenpass::protocol::CryptoServer`
- Pros: audited code path, identical wire format to upstream Rosenpass binary
- Cons: pulls in tokio + a lot of unused server-side code (mitigated by
  `--no-default-features`)

#### Option B — Hand-roll the protocol against the spec

- Implement the §4.2 frame format directly using `pqcrypto-*` + `sha2` + `hmac`
- Pros: minimal dep surface, every byte auditable
- Cons: cryptographer audit MANDATORY before shipping; risk of subtle errors
  in transcript hashing that wreck the security proof

**Decision:** Option A. We are not in the business of writing post-quantum
protocols. Use upstream code paths.

Tasks:

- [ ] Vendor `rosenpass-cipher-traits`, `rosenpass-ciphers`, `rosenpass`
      (protocol module only) under `native/vendor/`
- [ ] Implement `initiate()` — build `InitHello`
- [ ] Implement `handle_response()` — process `RespHello`, derive PSK
- [ ] Persist client static keypair via Android Keystore-wrapped EncryptedFile
      (NOT in plaintext SharedPreferences; the secret key is ~13 KB)
- [ ] Add `cargo test --release -- --ignored` to CI to exercise the slow KEM tests

### ⏳ M3 — Server-side wiring (separate repo: `birdo-vpn-server`)

The handshake needs an endpoint to talk to. Without this, M2 is useless.

- [ ] Each VPN node generates its own Classic McEliece keypair on first boot
      (`rosenpass gen-keys --secret-key /etc/rosenpass/sk`)
- [ ] Node runs `rosenpass exchange-config /etc/rosenpass/config.toml` as a
      systemd service listening on UDP/9999 (already deployed per
      `deploy/monitoring/rosenpass.toml` — verify it's actually live in prod)
- [ ] API `/connect` response populates `rosenpassPublicKey` (the node's static
      McEliece PK, base64) and `rosenpassEndpoint` (host:port)
- [ ] WireGuard config on the node has `PresharedKey` slot per peer that
      `rosenpass` daemon updates atomically every 120 s
- [ ] Verify with: `journalctl -u rosenpass -f` — should see "rekey OK" lines

### ⏳ M4 — Mobile integration

Wire the JNI into the actual VPN connect flow.

- [ ] `RosenpassManager` switches from reflection lookup to direct
      `RosenpassNative` calls (delete the `Class.forName(...)` block)
- [ ] On first install: generate + persist keypair (one-time, ~hundreds of ms)
- [ ] On connect: send `InitHello` UDP to `rosenpassEndpoint`, wait for
      `RespHello` with 5 s timeout, derive PSK, hand to `WireGuardTunnel`
- [ ] Re-key timer: every 120 s, repeat the exchange and atomically swap the
      PSK on the live wg interface via `wg set <iface> peer <pk> preshared-key /dev/stdin`
- [ ] Telemetry: increment `rosenpass.handshake.success` /
      `rosenpass.handshake.failure_<reason>` counters (no key material)
- [ ] Settings UI: "Quantum-resistant key exchange: ✓ Active (rekey in 47s)"

### ⏳ M5 — Audit + rollout

- [ ] External cryptographer review of `handshake.rs` against the Rosenpass
      whitepaper (budget ~1 week of an experienced reviewer's time)
- [ ] Penetration test: confirm an attacker MitM-ing the TLS API connection
      cannot derive the PSK
- [ ] Internal load test: 100 concurrent clients re-keying every 120 s
      against a single node — measure CPU + memory + handshake latency
- [ ] Staged rollout: 1% → 10% → 50% → 100% over 2 weeks, monitoring
      `rosenpass.handshake.failure_*` counters
- [ ] Update marketing copy to "bilateral post-quantum" only after this
      ships at 100%

---

## Threat model and audit checklist

### What the M2+ implementation defends against

- ✅ **Harvest Now, Decrypt Later (HNDL):** even if a future CRQC breaks the
      TLS X25519 used for the API call AND the WireGuard Curve25519 in the
      handshake, the PSK was never transmitted over a quantum-vulnerable
      channel — it was derived in-place via Classic McEliece + ML-KEM.
- ✅ **Compromise of long-lived static key:** the ML-KEM ephemeral component
      provides forward secrecy within the 120 s re-key window.
- ✅ **Active downgrade by network attacker:** the Rosenpass MAC binds the
      transcript so a tamper produces an invalid frame.

### What it does NOT defend against

- ❌ **Compromised server static key:** the node's McEliece secret key
      is the trust anchor. If exfiltrated, the attacker can decrypt all
      future PSKs derived against that node. Rotation policy: TBD with M3.
- ❌ **Side channels on the device:** Classic McEliece reference impls are
      not constant-time on all targets. Risk for PSK key material leakage
      via timing on shared-tenant phones is **non-zero**. Mitigation: re-key
      every 120 s caps the leak window.
- ❌ **Compromise of the JNI loader path:** an attacker with code execution
      on the device can replace `librosenpass_jni.so`. Mitigation: APK
      signature verification on install (out of our hands; Play Store does
      this) and `System.loadLibrary` from the APK's read-only zip section.

### Audit deliverables (required before flipping default-on)

- [ ] Independent review of `handshake.rs` line-by-line vs. Rosenpass §4.2
- [ ] `cargo audit` clean (no known vulns in dep tree)
- [ ] `cargo deny check` for license + supply-chain compliance
- [ ] Reproducible build verified by a third party (cargo-ndk output is
      deterministic given pinned toolchain + `Cargo.lock`)
- [ ] Threat model document signed off by Birdo security lead

---

## Honest disclaimers

1. The current main branch and v1.3.20 production app **do not** implement
   bilateral post-quantum key exchange. They use the server-provided PSK
   over TLS, which is **not** safe against HNDL.
2. Marketing material that claims "post-quantum" today is technically
   accurate only in the sense that *if you trust the server's TLS endpoint
   right now*, the PSK provides PQ protection of the WG tunnel against
   a *future* quantum attacker who only has the WG handshake. It does NOT
   protect against a passive attacker recording your TLS API call today.
3. The README claim of "Quantum-ready" is fair. "Quantum-resistant" /
   "post-quantum" should wait until M5 ships.
