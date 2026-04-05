# Code Signing with Sigstore

## Overview

Birdo VPN Mobile Client uses [Sigstore](https://www.sigstore.dev/) for keyless code signing
of release artifacts. Every APK, AAB, and iOS build artifact produced by CI is signed with
`cosign sign-blob` using GitHub's OIDC identity token.

This produces a `.sigstore` bundle containing:

- A **Fulcio certificate** proving the artifact was built by GitHub Actions from this repo
- A **Rekor transparency log entry** -- a tamper-proof public record of the signing event

## How It Works

### Android

```
GitHub Actions triggers
  -> Gradle builds release APK + AAB
  -> Android Keystore signs the APK/AAB (required by Android OS)
  -> cosign sign-blob --yes --bundle <file>.sigstore <file>
    -> Fulcio issues short-lived cert (GitHub OIDC identity)
    -> Signature recorded in Rekor transparency log
    -> .sigstore bundle uploaded as CI artifact
```

Android APKs require a standard Android signing key (stored as a GitHub secret).
Sigstore is layered **on top** of Android signing to provide independent provenance
verification -- you can verify which repo and workflow produced the APK, not just
that it was signed with our key.

### iOS

iOS builds are currently unsigned (no Apple Developer Program enrollment yet).
The shared KMP framework and iOS app binary are built and tested in CI, but
distribution signing will be added when App Store submission begins.

## Two Layers of Signing (Android)

| Layer | Purpose | Verifier |
|-------|---------|----------|
| **Android Keystore** | Required by Android OS to install APKs | Android OS verifies on install |
| **Sigstore** | Proves the APK was built from this repo's CI | Users verify with `cosign` |

The Android signing key proves the APK was signed by us. Sigstore proves it was
built from a specific commit in this repository by GitHub Actions -- not built locally
or tampered with after signing.

## Google Play vs Direct APK

- **Google Play:** Google re-signs the app with their own key (App Signing by Google Play).
  The Sigstore signature applies to the AAB uploaded to Play Console, not the final APK
  distributed by Google Play.
- **Direct APK (GitHub Releases):** The APK is signed with our Android Keystore AND
  has a Sigstore bundle. Users can verify both.

## Verifying Signatures

See [VERIFICATION.md](./VERIFICATION.md) for step-by-step instructions.

Quick verify:

```bash
cosign verify-blob \
  --bundle BirdoVPN-release.apk.sigstore \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  --certificate-identity-regexp "github.com/BirdoVPN/" \
  BirdoVPN-release.apk
```

## CI Configuration

| Workflow | Artifacts Signed |
|----------|-----------------|
| `android.yml` (push to main) | Release APK + Release AAB + SHA256SUMS.txt |
| `ios.yml` | Build artifacts uploaded (unsigned -- Sigstore signing planned) |

Both workflows use pinned action SHAs and minimal permissions.
The `id-token: write` permission is required for Sigstore's Fulcio OIDC flow.
