# Workout Tracker development instructions

The primary development target is the owner's Pixel over Android Wireless Debugging. The Windows Android emulator is the fallback test target.

For requested application changes:

1. Implement the change with the smallest coherent scope.
2. Run the relevant automated tests and Android lint.
3. Build the APK.
4. Unless the user says not to deploy, run `./scripts/dev-deploy.sh` to install the consistently signed release over Wi-Fi and launch it on the Pixel.
5. If the saved wireless endpoint is unavailable, do not uninstall the app or clear its data. Report that `./scripts/wifi-connect.sh` must be rerun with the current endpoint.

Use `./scripts/dev-deploy.sh --emulator` when phone deployment is unavailable or when emulator validation is specifically useful. Never commit `.keys`, `.tools`, `.dev-state`, APKs, or local workout data.
