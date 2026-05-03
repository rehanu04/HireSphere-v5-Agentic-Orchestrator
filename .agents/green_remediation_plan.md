# Green Remediation Plan (S24 Ultra / LiteRT Focus)

This plan outlines the required changes to optimize the Kotlin/Compose frontend of ResumeMatchV2 (specifically `LiveInterviewScreen.kt`) to eliminate energy leaks, adopt thermal-aware scheduling, and leverage the S24 Ultra's NPU via LiteRT.

## User Review Required

> [!WARNING]
> Migrating inference from the cloud (Gemini via network) to local LiteRT (NPU) will significantly reduce cloud costs and battery drain but will require packaging a local model (e.g., Gemma 2B and Whisper TFLite) in the APK or downloading it at runtime. Do you approve adding LiteRT dependencies and managing model downloads?

## Proposed Changes

### Frontend / UI Layer

#### [MODIFY] [LiveInterviewScreen.kt](file:///c:/Users/rehan/AndroidProjects/ResumeMatchV2/app/src/main/java/com/rehanu04/resumematchv2/ui/LiveInterviewScreen.kt)
*   **Energy Leak 1 - Infinite Recomposition in ParticleBlobOrb:** The `LaunchedEffect(Unit)` loop runs `while(true)` using `withFrameNanos` constantly updating rotation and wave properties, even when the UI is obscured or the interview is concluded.
    *   **Fix:** Tie the loop to the `Lifecycle.State.RESUMED` state. Stop the animation completely when the interview concludes or when `isThinking`, `isTalking`, and `isListening` are all false.
    *   **Estimated Savings:** Prevents CPU/GPU spin-locking, saving ~15-20% battery drain over a 5-minute interview.
*   **Energy Leak 2 - StarfieldBackground Constant Redraw:** The background uses `infiniteRepeatable(tween(100000...))` which forces Compose to constantly invalidate and redraw the canvas.
    *   **Fix:** Throttle the background animation framerate or switch to a static baked background during thermal throttling states.
*   **Energy Leak 3 - Continuous Microphone Hot-Loop:** The microphone is kept open continuously via `speechRecognizer?.startListening(intent)` looping on errors and silence.
    *   **Fix:** Implement push-to-talk or use voice activity detection (VAD) via LiteRT before opening the full Android `SpeechRecognizer` pipeline to prevent the audio daemon from burning power.

### Inference Layer (LiteRT NPU Offloading)

#### [MODIFY] [LiveInterviewScreen.kt](file:///c:/Users/rehan/AndroidProjects/ResumeMatchV2/app/src/main/java/com/rehanu04/resumematchv2/ui/LiveInterviewScreen.kt)
*   **Cloud Reliance to NPU Offloading:** Currently, transcribed text is sent to `apiBaseUrl/v1/ai/live-interview` to get an AI response. Sending continuous network requests (radio wake-locks) is highly energy-inefficient.
    *   **Fix:** Introduce LiteRT (TensorFlow Lite) for local LLM inference (e.g., Gemma 2B or a smaller quantized model) running on the S24 Ultra's NPU using the NNAPI/LiteRT delegate.
    *   **Estimated Savings:** Eliminates cellular/Wi-Fi radio wake-locks during the interview, significantly dropping thermal output and extending battery life by an estimated 25-30% per session.

## Open Questions

1.  **Thermal Throttling API:** Do you want to integrate the Android `PowerManager` Thermal API to dynamically downgrade the `ParticleBlobOrb` complexity (fewer particles) when the device gets hot?
2.  **LiteRT Model Size:** For local NPU offloading, what is the maximum model size limit we should target (e.g., <2GB for a quantized 2B model)?

## Verification Plan

### Automated Tests
- Profile the app using Android Studio Energy Profiler to compare baseline power usage vs. post-remediation power usage.
- Run UI tests verifying that the `while(true)` loops suspend correctly when the Compose lifecycle moves to `PAUSED`.

### Manual Verification
- Deploy to an S24 Ultra and run a 5-minute mock interview while monitoring thermal output and battery drop percentage.
- Verify that inference tasks successfully delegate to the NPU via LiteRT logs.
