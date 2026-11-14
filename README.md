# Spear Optimizer (Fabric, Minecraft 26.2)

Mod client-side untuk tombak (spear): menampilkan status charge, indikator siap yang
memperhitungkan ping/lag server, dan estimasi damage di HUD.

## Fitur
- Status charge: Charging -> Sync... -> READY -> Released (bar bergetar saat siap)
- Kompensasi lag: bar memakai delay tombak + ping (dalam tick). Garis putih = saat client
  menganggap siap; "Sync..." berarti server kemungkinan belum menganggap charge selesai.
- Preview damage (estimasi), kecepatan (blocks/detik), ping, dan level Lunge.
- Tanpa mixin, tanpa paket jaringan: hanya perlu dipasang di client.

## Syarat
- Minecraft 26.2, Fabric Loader >= 0.19.3, Fabric API untuk 26.2, Java 25

## Build
    gradle wrapper --gradle-version 9.5.1     # sekali saja, kalau belum ada gradlew
    ./gradlew build                           # hasil: build/libs/spear-optimizer-1.0.0.jar
    ./gradlew runClient                       # tes langsung

Kalau Fabric API / Loom tidak ditemukan, sesuaikan versi di gradle.properties
(cek https://fabricmc.net/develop).
