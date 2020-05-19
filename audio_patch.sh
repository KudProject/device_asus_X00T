cp device/asus/X00T/patches/0001-audio-hal-restore-tfa98xx-logic-for-asus-X00T.patch hardware/qcom-caf/msm8998/audio/tfa.patch
cd hardware/qcom-caf/msm8998/audio/
git apply -p1 < tfa.patch
