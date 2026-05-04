# BirdoVPN — Google Play Store Listing

> **How to use this file**
> Each section maps 1-to-1 to a field in the Google Play Console (Store Presence → Main Store Listing).
> Fields marked ⚠️ need action before submission. Fields marked ✅ are confirmed ready.

---

## App Identity

| Field | Value | Limit | Status |
|---|---|---|---|
| App name | `BirdoVPN` | 30 chars (used 8) | ✅ |
| Short description | *see below* | 80 chars | ⚠️ Needs update |
| Default language | English (United States) | — | ✅ |
| Category | **Tools** | — | ✅ |
| Tags (3 max) | `VPN`, `Privacy`, `WireGuard` | — | ✅ |

---

## Short Description  *(80 chars max — appears in search results)*

```
Sovereign WireGuard® VPN with kill switch, zero logs & split tunneling.
```
**69 chars** — safely within limit.

> **Why this wording?**
> The previous version contained "no ads" which triggers Play's *price/promotion keywords* policy flag.
> This version states the same value proposition using feature names instead of promotional negatives.
> Do NOT use phrases like: "free", "best", "#1", "no ads", "buy now", "limited offer", "discount".

---

## Full Description  *(4000 chars max — HTML not allowed, plain text only)*

```
BirdoVPN keeps your internet private with WireGuard®, the most modern and audited open-source VPN protocol available. Connect in under a second and browse at near-native speeds — no throttling, no interruptions.

Your privacy, your data. Birdo is built on a zero-activity-log architecture. We cannot see which websites you visit, which apps you use, what files you download, or when you connect. This is not just a legal promise — it is a technical design decision baked into the server infrastructure.

─── PROTECTION FEATURES ──────────────────────────

Kill Switch — All internet traffic is immediately blocked if the VPN drops unexpectedly. Your real IP address never leaks to websites, ISPs, or advertisers.

Split Tunneling — Choose which apps use the encrypted VPN tunnel and which bypass it. Stream local content with local apps while keeping everything else protected.

One-Tap Connect — A single tap activates your encrypted tunnel. Enable Auto-Connect to protect your traffic the moment the app opens.

Global Server Network — Low-latency nodes across Europe, North America, and Asia. Servers are load-balanced to keep speeds fast and consistent.

Quick Settings Tile — Toggle your VPN directly from the Android notification shade without opening the app.

WireGuard® Protocol — The fastest and most-audited modern VPN protocol. Trusted by the Linux kernel, connects in milliseconds, and uses state-of-the-art cryptography.

Zero Activity Logs — We do not record your browsing history, connection timestamps, assigned IP addresses, traffic destinations, or DNS queries. Period.

─── ADVANCED SETTINGS ────────────────────────────

Multi-Hop (Double VPN) — Route traffic through two independent VPN servers for maximum anonymity. Available on premium plans.

Custom DNS — Override the default DNS resolver with your own server (e.g. 1.1.1.1, 9.9.9.9, or a self-hosted resolver). Prevents DNS leaks at the source.

Local Network Sharing — Keep access to printers, NAS drives, and other local network devices while the VPN tunnel is active.

Port Forwarding — Expose a port through your VPN tunnel for hosting, remote access, or peer-to-peer applications.

Adjustable MTU and Port — Fine-tune WireGuard parameters for restrictive networks and firewalls that block standard VPN ports.

─── POST-QUANTUM READY ───────────────────────────

Birdo VPN is deploying Rosenpass alongside WireGuard to provide post-quantum encryption — protection that remains secure even against future quantum computers. Server infrastructure is live; full client integration is arriving in an upcoming update.

─── OPEN AND TRANSPARENT ─────────────────────────

Our server architecture, protocol choices, and privacy model are publicly documented. We believe trust is earned through transparency, not through marketing promises. The Birdo app contains no advertising SDKs and no third-party analytics or tracking libraries.

─── PLANS ────────────────────────────────────────

Free tier — Connect to our network with core WireGuard protection and zero logs.
RECON plan — Unlocks Multi-Hop, Port Forwarding, priority servers, and early access to post-quantum features.

Whether you are on public Wi-Fi, travelling internationally, working remotely, or simply want an encrypted private tunnel, BirdoVPN protects you with one tap.

Download now and take control of your internet.

WireGuard is a registered trademark of Jason A. Donenfeld.
```

**Character count: ~2 820 / 4 000** — room to add localized feature highlights or an FAQ block.

---

## Graphic Assets

### Required sizes (Play Store rules)
| Asset | Required dimensions | Ratio | Status |
|---|---|---|---|
| App icon | 512 × 512 px, PNG, no alpha in background | Square | ✅ `screenshots/play/store-assets/icon-512.png` |
| Feature graphic | 1024 × 500 px, PNG or JPG | — | ✅ `screenshots/play/store-assets/feature-1024x500.png` |
| Phone screenshots | Min 2, max 8 per language · 9:16 or 16:9 · 1080–3840 px per side | 9:16 | ✅ `screenshots/play/phone/*.png` (1080 × 1920) |
| 7" tablet screenshots | Min 1, max 8 · 16:9 or 9:16 · 320–3840 px per side | 16:9 | ✅ `screenshots/play/tablet/*.png` (1920 × 1080) |
| 10" tablet screenshots | Min 1, max 8 · 16:9 or 9:16 · **1080 px min** per side | 16:9 | ✅ Same files as 7" — 1920 × 1080 satisfies the 1080 px minimum |

> The 7" and 10" tablet slots can use **identical images** as long as both dimensions are ≥ 1080 px and the ratio is 16:9 or 9:16. Our 1920 × 1080 tablet screenshots satisfy this.

### Screenshot order (recommended — most impactful first)
| # | Filename | Content | Why it matters |
|---|---|---|---|
| 1 | `01_home.png` | Home screen: globe, power button, server row | Hero shot — first impression |
| 2 | `02_server_list.png` | Server browser / country selection sheet | Shows global reach |
| 3 | `03_settings_security.png` | Settings: Kill Switch ON, Auto-Connect | Highlights security features |
| 4 | `04_settings_features.png` | Settings scrolled: Split Tunneling, Multi-Hop | Shows premium features |
| 5 | `05_vpn_settings.png` | VPN Settings sub-screen: DNS, WireGuard, MTU | Shows technical depth |
| 6 | `06_profile.png` | Profile: plan badge, subscription, account | Shows subscription context |
| 7 | `07_subscription.png` | Plan comparison / subscription screen | Communicates value tiers |
| 8 | `08_connecting.png` | Connecting animation or VPN permission dialog | Shows real connection flow |

### To regenerate screenshots
```powershell
# From w:\vpn\birdo-client-mobile
powershell.exe -ExecutionPolicy Bypass -File scripts\capture-screenshots.ps1 -Form phone
powershell.exe -ExecutionPolicy Bypass -File scripts\capture-screenshots.ps1 -Form tablet
```

---

## Pricing & Distribution

| Field | Value |
|---|---|
| Price | Free |
| In-app purchases | Yes — RECON plan subscription *(must be declared in Data Safety)* |
| Contains ads | No |
| Available in all countries | Recommended: Yes, **except** where VPN apps are legally restricted (Russia, China, Iran, North Korea, UAE — confirm with legal) |

---

## Content Rating  *(Questionnaire in Play Console → Policy → App content)*

Answer the IARC questionnaire as follows:

| Question | Answer | Reason |
|---|---|---|
| Violence | None | Networking utility, no violent content |
| Sexual content | None | No such content |
| Language | None | No profanity |
| Controlled substances | None | Irrelevant |
| User-generated content (UGC) | No | Users cannot create/share content in-app |
| User interaction | No social features | No chat, no profiles visible to others |
| Shares location | No | VPN masks location but does not share it |
| Personal communications | No | Not a messaging app |

**Expected rating: Everyone (E) / PEGI 3**

---

## Privacy & Data Safety  *(Play Console → Policy → Data Safety)*

### Permissions the app declares
| Permission | Why | Must declare in Data Safety? |
|---|---|---|
| `INTERNET` | VPN traffic routing | Yes — network requests |
| `BIND_VPN_SERVICE` | Android VPN tunnel setup | Yes |
| `FOREGROUND_SERVICE` | Persistent VPN notification | Yes |
| `RECEIVE_BOOT_COMPLETED` | Auto-Connect on device startup | Yes |
| `POST_NOTIFICATIONS` (Android 13+) | Connection status notifications | Yes |
| `CHANGE_NETWORK_STATE` | Kill switch network management | Yes |

### Data Safety form answers
| Data type | Collected? | Shared? | Notes |
|---|---|---|---|
| Email address | Yes — required for account | No third parties | Used for authentication only |
| User IDs (JWT token) | Yes — session only | No | Ephemeral, never persisted to disk by Birdo |
| App activity / usage | No | No | Zero-log design |
| Web browsing history | No | No | Never recorded |
| Location | No | No | VPN masks real IP but location is never sent to Birdo |
| Device identifiers | No | No | |
| Crash logs | No (confirm — if Crashlytics absent) | No | |
| Financial info | Subscription billing handled by payment processor (Google Play IAP) | Per Google Play billing policy | |

> ⚠️ **Action required**: Confirm whether the app uses Crashlytics, Firebase, or any analytics SDK. If so, declare crash logs and update accordingly.

### VPN-specific declaration
Google Play requires apps in the VPN category to **not** collect or misuse sensitive data routed through the VPN tunnel. Complete the "VPN app" questionnaire section when it appears.

Include this statement (or equivalent) in your **Privacy Policy**:
> "BirdoVPN does not log, store, or transmit any records of your network activity, browsing history, DNS queries, traffic metadata, IP addresses assigned to you, or the timing and duration of your VPN sessions."

---

## Privacy Policy  *(required for all apps)*

| Field | Value |
|---|---|
| Privacy Policy URL | `https://birdo.app/privacy` *(confirm this page exists and is publicly accessible)* |
| Terms of Service URL | `https://birdo.app/terms` |

> ⚠️ The privacy policy page must be live, written in plain language, and specifically address VPN data handling. Generic generator policies will likely fail review.

---

## Store Contact Information

| Field | Value |
|---|---|
| Developer name | Birdo *(or your registered legal entity)* |
| Email | `support@birdo.app` *(must be a monitored address — Google contacts you here for policy issues)* |
| Website | `https://birdo.app` |
| Physical address | Required if publishing paid apps — provide your registered business address |

---

## Release Notes  *(What's New — 500 chars max per release)*

Template for v1.0 initial release:
```
BirdoVPN is here.

- WireGuard® VPN with one-tap connect
- Kill Switch keeps you protected if the tunnel drops
- Split Tunneling — route only the apps you choose
- Custom DNS, adjustable MTU and port
- Zero activity logs

More features including Multi-Hop and post-quantum encryption arriving soon.
```

Template for a feature update:
```
v1.x update

- [NEW] Multi-Hop: route through two servers for extra privacy
- [NEW] Post-quantum encryption with Rosenpass (beta)
- [FIX] Kill Switch behaviour on Android 14
- [IMPROVE] Faster server list loading

Full changelog: https://birdo.app/changelog
```

---

## Pre-Launch Checklist

- [ ] All 8 phone screenshots uploaded (1080 × 1920)
- [ ] All 8 tablet screenshots uploaded (1920 × 1080) to BOTH 7" and 10" slots
- [ ] App icon uploaded (512 × 512)
- [ ] Feature graphic uploaded (1024 × 500)
- [ ] Short description updated (no promotion keywords)
- [ ] Full description reviewed (no HTML tags, no competitor names without legal clearance)
- [ ] Privacy Policy URL live and accessible
- [ ] Content rating questionnaire completed
- [ ] Data Safety form completed
- [ ] VPN app declaration submitted
- [ ] In-app subscription products created in Play Console (if applicable)
- [ ] App tested on a real device (not just emulator) before submission
- [ ] Target API level meets current Play requirement (API 34+ as of Aug 2024)
- [ ] 64-bit APK / AAB uploaded
- [ ] Signed with upload key (not debug key)

---

## Policy Flags to Watch

| Flag | Risk | Mitigation |
|---|---|---|
| VPN category | High scrutiny | Complete VPN declaration; privacy policy must be accurate |
| "zero logs" claim | Medium | Must be verifiable — document your server architecture |
| Subscription IAP | Medium | Clearly disclose subscription terms before purchase prompt |
| `BIND_VPN_SERVICE` | Triggers manual review | Normal for VPN apps — expect 3–7 day review window |
| Background network access | Must be declared | Covered by foreground service + VPN service permissions |
