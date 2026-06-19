# Project IDX Configuration - Drum Stroke Counter
# This file helps Google Project IDX understand your project structure

{ pkgs, ... }: {
  # Android SDK tools
  packages = with pkgs; [
    jdk17
    android-tools
  ];

  # Environment setup
  env = {
    ANDROID_HOME = "$HOME/android-sdk";
  };
}
