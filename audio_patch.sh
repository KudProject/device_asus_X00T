cp device/asus/X00T/patches/0001-X00T-Update-tfa98xx-audio-patch-for-R.patch hardware/qcom-caf/sdm660/audio/tfa.patch
cd hardware/qcom-caf/sdm660/audio/
git apply -p1 < tfa.patch > /dev/null 2>&1
