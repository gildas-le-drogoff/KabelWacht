<!--
SPDX-FileCopyrightText: 2026 Tobias Mueller and KabelWacht contributors
SPDX-License-Identifier: AGPL-3.0-or-later
-->

# KabelWacht privacy policy

KabelWacht is a WireGuard VPN client for Android. It does not collect, transmit,
sell, or share any personal or device data. There are no accounts, no analytics,
no advertising, no crash reporting, and no servers operated by the developer.
Everything the app knows stays on your device.

This policy applies to KabelWacht (`com.github.muelli.kabelwacht`) however it
was installed, whether from Google Play, F-Droid, or GitHub.

## What the app stores, and where

- **Tunnel configurations** you create, import, or scan, including their
  private keys. They are kept in the app's private storage on your device.
- **Settings and run conditions**, such as the Wi-Fi network names (SSIDs) you
  enter as rules for starting or stopping a tunnel, and which tunnel each home
  screen widget controls. Also kept in the app's private storage.

Android's cloud backup is disabled for the app, so none of this leaves your
device through a backup. Uninstalling the app deletes it.

## Network connections

KabelWacht connects only to the WireGuard servers named in your own tunnel
configurations. Your traffic goes through the tunnel to the server you chose,
and the operator of that server can see it as with any VPN. The developer
receives nothing.

## Permissions

| Permission | Why |
| --- | --- |
| Internet, network state | To establish the VPN tunnels you configure. |
| Wi-Fi state, location | To read the name of the current Wi-Fi network, but only if you set up run conditions based on Wi-Fi names. Android requires location permission for this. Your location is never read, stored, or sent anywhere. |
| Camera | To scan a tunnel's QR code when you choose to. Images are processed on the device and not stored. |
| Biometrics | To confirm it is you, using the device lock, before a tunnel configuration with its private key is exported. Handled entirely by Android; the app never sees biometric data. |
| Notifications, foreground service | To show that a tunnel or its automation is running. |
| Run at start-up | To restore your tunnels and run conditions after a reboot. |

## Export and import

When you export a tunnel configuration, it is written to a location you pick
using Android's file picker. When you import one, the app reads only the file
you select. What happens to an exported file after that is up to you; keep in
mind that it contains the tunnel's private key.

## Children

KabelWacht is not directed at children and collects no data from anyone.

## Changes

Changes to this policy are made in the public source repository, so every
revision is visible in its history:
<https://github.com/muelli/KabelWacht/commits/main/PRIVACY.md>

## Contact

Questions about privacy can be raised as an issue at
<https://github.com/muelli/KabelWacht/issues> or sent privately to the
maintainer as described in [SECURITY.md](SECURITY.md).

Last updated: 15 October 2026
