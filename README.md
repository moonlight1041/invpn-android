# InVPN — Android client

InVPN is an Android VPN client.

## Attribution / Credits

This project is a derivative work based on **sing-box for Android (SFA)** by nekohasekai / SagerNet,
licensed under the **GNU General Public License v3.0**.

- Upstream app: https://github.com/SagerNet/sing-box-for-android
- Core engine: https://github.com/SagerNet/sing-box (sing-box, GPLv3)

### Changes from upstream
- Rebranded: application id `com.invpn.app`, app name "InVPN" (does **not** use the upstream name).
- (in progress) Authenticated in-app configuration delivery; simplified one-button UI.

## Building
Built in CI (GitHub Actions, see `.github/workflows/build.yml`): builds `libbox.aar` from sing-box via
gomobile, then the `other` flavor APK. Android-only.

## License
GNU General Public License v3.0 or later — see [LICENSE](LICENSE). Source is published in compliance with the GPL.
