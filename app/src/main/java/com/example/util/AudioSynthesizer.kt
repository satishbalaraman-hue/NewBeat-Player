package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.Track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sin

object AudioSynthesizer {
    private const val TAG = "AudioSynthesizer"

    fun generateDemoTracks(context: Context): List<Track> {
        val list = mutableListOf<Track>()
        val directory = File(context.cacheDir, "demo_music")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        // 1. Cozy Lofi Ambient (Optimized to 44.1kHz, 16-bit, 3s loop to prevent start delay)
        val track1 = File(directory, "lofi_ambient_v2.wav")
        if (!track1.exists()) {
            synthesizeWav(
                file = track1,
                sampleRate = 44100,
                isStereo = true,
                bitDepth = 16,
                durationSeconds = 3,
                synthType = SynthType.LOFI_AMBIENT
            )
        }
        list.add(
            Track(
                id = 1001L,
                title = "Nova Horizon (Lofi Ambient)",
                artist = "Sola Eclipse",
                album = "Cosmic Dust",
                path = track1.absolutePath,
                uri = Uri.fromFile(track1),
                durationMs = 3000L,
                size = track1.length(),
                genre = "Ambient",
                isDemo = true,
                sampleRateKhz = 44.1f,
                bitDepth = 16,
                format = "WAV"
            )
        )

        // 2. Midnight Arpeggio (Optimized to 44.1kHz, 16-bit, 3s loop)
        val track2 = File(directory, "midnight_arp_v2.wav")
        if (!track2.exists()) {
            synthesizeWav(
                file = track2,
                sampleRate = 44100,
                isStereo = true,
                bitDepth = 16,
                durationSeconds = 3,
                synthType = SynthType.MID_ARP
            )
        }
        list.add(
            Track(
                id = 1002L,
                title = "Starlight Sonata (Arpeggio)",
                artist = "Synthetix",
                album = "Neon Skies",
                path = track2.absolutePath,
                uri = Uri.fromFile(track2),
                durationMs = 3000L,
                size = track2.length(),
                genre = "Electronic",
                isDemo = true,
                sampleRateKhz = 44.1f,
                bitDepth = 16,
                format = "WAV"
            )
        )

        // 3. Deep Pulse Sub (Optimized to 44.1kHz, 16-bit, 3s loop)
        val track3 = File(directory, "deep_pulse_v2.wav")
        if (!track3.exists()) {
            synthesizeWav(
                file = track3,
                sampleRate = 44100,
                isStereo = false,
                bitDepth = 16,
                durationSeconds = 3,
                synthType = SynthType.DEEP_PULSE
            )
        }
        list.add(
            Track(
                id = 1003L,
                title = "Abyssal Pulse",
                artist = "Mute Echo",
                album = "Tectonic Resonance",
                path = track3.absolutePath,
                uri = Uri.fromFile(track3),
                durationMs = 3000L,
                size = track3.length(),
                genre = "Minimal Synth",
                isDemo = true,
                sampleRateKhz = 44.1f,
                bitDepth = 16,
                format = "WAV"
            )
        )

        // 4. Retro Chiptune Loop (Optimized to 44.1kHz, 16-bit, 3s loop)
        val track4 = File(directory, "retro_chiptune_v2.wav")
        if (!track4.exists()) {
            synthesizeWav(
                file = track4,
                sampleRate = 44100,
                isStereo = true,
                bitDepth = 16,
                durationSeconds = 3,
                synthType = SynthType.RETRO_CHIP
            )
        }
        list.add(
            Track(
                id = 1004L,
                title = "Binary Bit-Hop",
                artist = "8-Bit Wizard",
                album = "Level Select",
                path = track4.absolutePath,
                uri = Uri.fromFile(track4),
                durationMs = 3000L,
                size = track4.length(),
                genre = "Chiptune",
                isDemo = true,
                sampleRateKhz = 44.1f,
                bitDepth = 16,
                format = "WAV"
            )
        )

        return list
    }

    enum class SynthType {
        LOFI_AMBIENT,
        MID_ARP,
        DEEP_PULSE,
        RETRO_CHIP
    }

    private fun synthesizeWav(
        file: File,
        sampleRate: Int,
        isStereo: Boolean,
        bitDepth: Int,
        durationSeconds: Int,
        synthType: SynthType
    ) {
        val numChannels = if (isStereo) 2 else 1
        val numSamples = sampleRate * durationSeconds
        val bytesPerSample = bitDepth / 8
        val subChunk2Size = numSamples * numChannels * bytesPerSample
        val chunkSize = 36 + subChunk2Size

        try {
            FileOutputStream(file).use { fos ->
                // RIFF custom wave header (44 bytes total)
                fos.write("RIFF".toByteArray())
                fos.write(intToByteArray(chunkSize))
                fos.write("WAVE".toByteArray())
                fos.write("fmt ".toByteArray())
                fos.write(intToByteArray(16)) // SubChunk1Size for PCM
                fos.write(shortToByteArray(1)) // AudioFormat (1 = PCM)
                fos.write(shortToByteArray(numChannels.toShort()))
                fos.write(intToByteArray(sampleRate))
                fos.write(intToByteArray(sampleRate * numChannels * bytesPerSample)) // ByteRate
                fos.write(shortToByteArray((numChannels * bytesPerSample).toShort())) // BlockAlign
                fos.write(shortToByteArray(bitDepth.toShort())) // BitsPerSample
                fos.write("data".toByteArray())
                fos.write(intToByteArray(subChunk2Size))

                // Synthesize & write samples
                val bufferSize = 4096
                val buffer = ByteArray(bufferSize)
                var bufferIdx = 0

                val pi2 = 2.0 * Math.PI

                for (s in 0 until numSamples) {
                    val t = s.toDouble() / sampleRate

                    // Synthesizer DSP depending on SynthType
                    val value: Double = when (synthType) {
                        SynthType.LOFI_AMBIENT -> {
                            // High quality relaxing ambient melody (chords of E major with manual vibrato)
                            val f1 = 164.81 // E3
                            val f2 = 246.94 // B3
                            val f3 = 329.63 // E4
                            val vibrato = 1.0 + 0.01 * sin(pi2 * 3.0 * t)
                            val signal = (sin(pi2 * f1 * vibrato * t) + 
                                          sin(pi2 * f2 * vibrato * t) * 0.5 + 
                                          sin(pi2 * f3 * vibrato * t) * 0.3) / 1.8
                            val envelope = sin(Math.PI * t / durationSeconds) // Soft fade in/out
                            signal * envelope
                        }
                        SynthType.MID_ARP -> {
                            // Synthetix Arpeggiator (retro electro notes cycling)
                            val notes = doubleArrayOf(261.63, 311.13, 392.00, 466.16) // Cm7 arp: C4, Eb4, G4, Bb4
                            val noteIdx = ((t * 4.0) % 4).toInt() // 4 notes per second (120 BPM)
                            val freq = notes[noteIdx]
                            // Combine a square/triangle synthesized sound
                            val square = if (sin(pi2 * freq * t) > 0.0) 0.3 else -0.3
                            val sineSub = sin(pi2 * (freq / 2) * t) * 0.5
                            val envelope = 1.0 - ((t * 4.0) % 1.0) // sharp attack, exponential decay for arp effect
                            (square + sineSub) * envelope * 0.4
                        }
                        SynthType.DEEP_PULSE -> {
                            // Deep ambient sub-bass pulse
                            val freq = 55.0 // A1 sub-bass
                            val pulse = sin(pi2 * freq * t) * sin(pi2 * 0.5 * t) // slow pulse envelope
                            pulse * 0.6
                        }
                        SynthType.RETRO_CHIP -> {
                            // Binary Bit-Hop chiptune sound (fast retro pulse)
                            val scale = doubleArrayOf(392.00, 440.00, 523.25, 587.33, 659.25, 783.99)
                            val noteIdx = ((t * 6.0) % 6).toInt()
                            val freq = scale[noteIdx]
                            val rawSquare = if (sin(pi2 * freq * t) > 0.0) 0.4 else -0.4
                            val crunch = if (((t * 12.0) % 1.0) < 0.2) 0.1 else 1.0 // pulse-width modulation gate
                            rawSquare * crunch * 0.25
                        }
                    }

                    // Convert double value [-1.0, 1.0] to specific PCM bytes
                    // Write to channel(s)
                    for (chan in 0 until numChannels) {
                        // For stereo: add slight phase offset or frequency shift to the secondary channel
                        val finalValue = if (isStereo && chan == 1) {
                            // Stereo widening by shifting phase or mixing 34% delay
                            value * 0.8 + 0.3 * sin(pi2 * 100.0 * t) * value
                        } else {
                            value
                        }

                        // Clamp channel value to safety
                        val clamped = finalValue.coerceIn(-1.0, 1.0)

                        if (bitDepth == 16) {
                            val sampleShort = (clamped * 32767.0).toInt().toShort()
                            val sb = shortToByteArray(sampleShort)
                            buffer[bufferIdx++] = sb[0]
                            buffer[bufferIdx++] = sb[1]
                        } else if (bitDepth == 24) {
                            // 24-bit PCM: write 3 bytes per sample
                            val valInt = (clamped * 8388607.0).toInt()
                            buffer[bufferIdx++] = (valInt and 0xFF).toByte()
                            buffer[bufferIdx++] = ((valInt shr 8) and 0xFF).toByte()
                            buffer[bufferIdx++] = ((valInt shr 16) and 0xFF).toByte()
                        }

                        // Flush buffer if full
                        if (bufferIdx >= bufferSize - 10) {
                            fos.write(buffer, 0, bufferIdx)
                            bufferIdx = 0
                        }
                    }
                }

                // Write remaining bytes
                if (bufferIdx > 0) {
                    fos.write(buffer, 0, bufferIdx)
                }
            }
            Log.d(TAG, "Generated synthetic high-res WAV track: ${file.name}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write synthesized wav file: ${file.name}", e)
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0x00FF).toByte(),
            ((value.toInt() and 0xFF00) shr 8).toByte()
        )
    }
}
