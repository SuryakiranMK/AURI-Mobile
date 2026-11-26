// AURI Mobile Core - Audio DSP Module
// Ported from AURI desktop: -14.0 LUFS broadcast loudness target, Soft Peak Limiter,
// and 10-band ISO octave equalizer.

use serde::{Deserialize, Serialize};

/// Target loudness matching YouTube Music standard (-14.0 LUFS)
pub const TARGET_LUFS: f64 = -14.0;

/// Maximum boost in dB for quiet recordings to preserve headroom
pub const MAX_BOOST_DB: f64 = 3.0;

/// Minimum attenuation in dB for excessively mastered tracks
pub const MIN_CUT_DB: f64 = -24.0;

/// Standard 10-band ISO octave center frequencies in Hz
pub const EQ_FREQUENCIES: [f32; 10] = [
    31.0, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
];

pub const EQ_BAND_LABELS: [&str; 10] = [
    "31Hz", "63Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz",
];

/// Calculate required gain adjustment in dB from YouTube Music's metadata `loudnessDb`.
pub fn calculate_loudness_gain_db(loudness_db: Option<f64>) -> Option<f64> {
    let gain = match loudness_db {
        Some(raw) if raw.is_finite() => (-raw).clamp(MIN_CUT_DB, MAX_BOOST_DB),
        _ => -4.5, // Intelligent fallback for tracks without loudness metadata
    };
    if gain.abs() >= 0.05 {
        Some(gain)
    } else {
        None
    }
}

/// Convert dB to linear amplitude multiplier
#[inline]
pub fn db_to_linear(gain_db: f64) -> f32 {
    (10.0_f64.powf(gain_db / 20.0)) as f32
}

/// A zero-allocation, soft-knee peak limiter.
#[derive(Debug, Clone)]
pub struct SoftLimiter {
    threshold: f32,
}

impl Default for SoftLimiter {
    fn default() -> Self {
        Self { threshold: 0.95 }
    }
}

impl SoftLimiter {
    pub fn new(threshold: f32) -> Self {
        Self {
            threshold: threshold.clamp(0.5, 0.99),
        }
    }

    #[inline]
    pub fn process_sample(&self, sample: f32) -> f32 {
        let abs = sample.abs();
        if abs <= self.threshold {
            sample
        } else {
            let excess = abs - self.threshold;
            let headroom = 1.0 - self.threshold;
            let compressed = (self.threshold + (headroom * excess) / (headroom * headroom + excess * excess).sqrt())
                .min(1.0 - 1e-6);
            if sample >= 0.0 {
                compressed
            } else {
                -compressed
            }
        }
    }

    pub fn process_buffer(&self, buffer: &mut [f32], gain_multiplier: f32) {
        let gain_multiplier = if gain_multiplier.is_finite() { gain_multiplier } else { 1.0 };
        if (gain_multiplier - 1.0).abs() < 0.001 {
            for s in buffer.iter_mut() {
                if s.abs() > self.threshold {
                    *s = self.process_sample(*s);
                }
            }
        } else {
            for s in buffer.iter_mut() {
                let amplified = *s * gain_multiplier;
                *s = self.process_sample(amplified);
            }
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
pub enum EqPreset {
    #[default]
    Flat,
    BassPunch,
    VocalClarity,
    Cinematic,
    Custom,
}

impl EqPreset {
    pub const ALL: [EqPreset; 5] = [
        EqPreset::Flat,
        EqPreset::BassPunch,
        EqPreset::VocalClarity,
        EqPreset::Cinematic,
        EqPreset::Custom,
    ];

    pub fn label(&self) -> &'static str {
        match self {
            EqPreset::Flat => "Audiophile Flat",
            EqPreset::BassPunch => "Deep Bass Punch",
            EqPreset::VocalClarity => "Vocal Clarity",
            EqPreset::Cinematic => "Cinematic Dynamic",
            EqPreset::Custom => "Custom EQ",
        }
    }

    pub fn gains(&self) -> [f32; 10] {
        match self {
            EqPreset::Flat => [0.0; 10],
            EqPreset::BassPunch => [5.5, 6.0, 4.0, 2.0, 0.5, -0.5, 0.0, 1.0, 2.0, 2.0],
            EqPreset::VocalClarity => [-2.0, -2.5, -1.0, 1.0, 2.5, 4.5, 3.5, 2.0, 1.0, 0.5],
            EqPreset::Cinematic => [6.0, 4.5, 2.0, 0.0, -1.0, 0.5, 2.0, 3.5, 4.5, 4.0],
            EqPreset::Custom => [0.0; 10],
        }
    }
}

#[derive(Debug, Clone, Copy, Default)]
pub struct BiquadCoeffs {
    b0: f32,
    b1: f32,
    b2: f32,
    a1: f32,
    a2: f32,
    identity: bool,
}

impl BiquadCoeffs {
    pub fn peaking(sample_rate: f32, f0: f32, gain_db: f32, q: f32) -> Self {
        if gain_db.abs() < 0.05 {
            return Self {
                b0: 1.0,
                b1: 0.0,
                b2: 0.0,
                a1: 0.0,
                a2: 0.0,
                identity: true,
            };
        }

        let nyquist = sample_rate * 0.495;
        let f0 = f0.clamp(10.0, nyquist);

        let a = 10.0_f32.powf(gain_db / 40.0);
        let w0 = 2.0 * std::f32::consts::PI * f0 / sample_rate;
        let cos_w0 = w0.cos();
        let sin_w0 = w0.sin();
        let alpha = sin_w0 / (2.0 * q);

        let b0 = 1.0 + alpha * a;
        let b1 = -2.0 * cos_w0;
        let b2 = 1.0 - alpha * a;
        let a0 = 1.0 + alpha / a;
        let a1 = -2.0 * cos_w0;
        let a2 = 1.0 - alpha / a;

        Self {
            b0: b0 / a0,
            b1: b1 / a0,
            b2: b2 / a0,
            a1: a1 / a0,
            a2: a2 / a0,
            identity: false,
        }
    }
}

#[derive(Debug, Clone, Copy, Default)]
pub struct BiquadState {
    s1: f32,
    s2: f32,
}

impl BiquadState {
    #[inline]
    pub fn process(&mut self, coeffs: &BiquadCoeffs, x: f32) -> f32 {
        if coeffs.identity {
            return x;
        }
        let y = coeffs.b0 * x + self.s1;
        self.s1 = coeffs.b1 * x - coeffs.a1 * y + self.s2;
        self.s2 = coeffs.b2 * x - coeffs.a2 * y;
        if self.s1.abs() < 1e-15 {
            self.s1 = 0.0;
        }
        if self.s2.abs() < 1e-15 {
            self.s2 = 0.0;
        }
        y
    }

    pub fn reset(&mut self) {
        self.s1 = 0.0;
        self.s2 = 0.0;
    }
}

#[derive(Debug, Clone)]
pub struct Equalizer {
    coeffs: [BiquadCoeffs; 10],
    states: [[BiquadState; 2]; 10],
    gains: [f32; 10],
    preamp_db: f32,
    sample_rate: u32,
    pub active: bool,
}

impl Default for Equalizer {
    fn default() -> Self {
        Self::new(48_000)
    }
}

impl Equalizer {
    pub fn new(sample_rate: u32) -> Self {
        let mut eq = Self {
            coeffs: [BiquadCoeffs::default(); 10],
            states: [[BiquadState::default(); 2]; 10],
            gains: [0.0; 10],
            preamp_db: 0.0,
            sample_rate,
            active: false,
        };
        eq.recalculate();
        eq
    }

    pub fn set_sample_rate(&mut self, rate: u32) {
        if self.sample_rate != rate && rate > 0 {
            self.sample_rate = rate;
            self.recalculate();
            self.reset_states();
        }
    }

    pub fn set_preamp(&mut self, preamp_db: f32) {
        let clamped = preamp_db.clamp(-12.0, 12.0);
        if (self.preamp_db - clamped).abs() >= 0.05 {
            self.preamp_db = clamped;
        }
        self.update_active();
    }

    pub fn set_gains(&mut self, gains: [f32; 10]) {
        let mut changed = false;
        for (i, &g) in gains.iter().enumerate() {
            let clamped = g.clamp(-12.0, 12.0);
            if (self.gains[i] - clamped).abs() >= 0.05 {
                self.gains[i] = clamped;
                changed = true;
            }
        }
        self.update_active();
        if changed {
            self.recalculate();
        }
    }

    fn update_active(&mut self) {
        self.active = self.preamp_db.abs() >= 0.05 || self.gains.iter().any(|g| g.abs() >= 0.05);
    }

    fn recalculate(&mut self) {
        let rate = self.sample_rate as f32;
        let q = 1.414_f32;
        for i in 0..10 {
            self.coeffs[i] = BiquadCoeffs::peaking(rate, EQ_FREQUENCIES[i], self.gains[i], q);
        }
    }

    pub fn reset_states(&mut self) {
        for band in &mut self.states {
            band[0].reset();
            band[1].reset();
        }
    }

    pub fn process_interleaved(&mut self, buffer: &mut [f32], channels: usize, sample_rate: u32) {
        self.set_sample_rate(sample_rate);
        if !self.active || channels == 0 {
            return;
        }

        let preamp_mult = if self.preamp_db.abs() >= 0.05 {
            db_to_linear(self.preamp_db as f64)
        } else {
            1.0
        };

        if channels == 1 {
            for sample in buffer.iter_mut() {
                let mut s = *sample * preamp_mult;
                for b in 0..10 {
                    s = self.states[b][0].process(&self.coeffs[b], s);
                }
                *sample = s;
            }
        } else {
            for frame in buffer.chunks_mut(channels) {
                let mut left = frame[0] * preamp_mult;
                for b in 0..10 {
                    left = self.states[b][0].process(&self.coeffs[b], left);
                }
                frame[0] = left;

                if frame.len() > 1 {
                    let mut right = frame[1] * preamp_mult;
                    for b in 0..10 {
                        right = self.states[b][1].process(&self.coeffs[b], right);
                    }
                    frame[1] = right;
                }
            }
        }
    }
}

/// Computes negative preamp headroom (in dB) equal to the highest positive EQ band boost,
/// preventing digital clipping before the soft-knee peak limiter.
pub fn compute_auto_headroom_db(gains: &[f32; 10]) -> f32 {
    let max_boost = gains.iter().copied().fold(0.0_f32, f32::max).max(0.0);
    if max_boost >= 0.05 { -max_boost } else { 0.0 }
}

/// Complete per-stream PCM DSP pipeline: 10-band ISO EQ + Auto-Headroom +
/// -14.0 LUFS Loudness Normalization + Soft-Knee Peak Limiter.
#[derive(Debug, Clone)]
pub struct AudioPipeline {
    eq: Equalizer,
    limiter: SoftLimiter,
    loudness_gain_db: f64,
    normalizer_enabled: bool,
    auto_headroom_enabled: bool,
    user_preamp_db: f32,
    gains: [f32; 10],
    scratch_f32: Vec<f32>,
}

impl AudioPipeline {
    pub fn new(sample_rate: u32) -> Self {
        Self {
            eq: Equalizer::new(sample_rate.max(8_000)),
            limiter: SoftLimiter::default(),
            loudness_gain_db: 0.0,
            normalizer_enabled: true,
            auto_headroom_enabled: true,
            user_preamp_db: 0.0,
            gains: [0.0; 10],
            scratch_f32: Vec::with_capacity(4096),
        }
    }

    pub fn configure(
        &mut self,
        gains: [f32; 10],
        user_preamp_db: f32,
        auto_headroom_enabled: bool,
        loudness_gain_db: f64,
        normalizer_enabled: bool,
    ) {
        self.gains = gains;
        self.user_preamp_db = user_preamp_db.clamp(-12.0, 12.0);
        self.auto_headroom_enabled = auto_headroom_enabled;
        self.loudness_gain_db = if loudness_gain_db.is_finite() {
            loudness_gain_db.clamp(MIN_CUT_DB, MAX_BOOST_DB)
        } else {
            -4.5
        };
        self.normalizer_enabled = normalizer_enabled;

        let headroom = if auto_headroom_enabled {
            compute_auto_headroom_db(&self.gains)
        } else {
            0.0
        };
        self.eq.set_gains(self.gains);
        self.eq.set_preamp(self.user_preamp_db + headroom);
    }

    pub fn reset(&mut self) {
        self.eq.reset_states();
    }

    pub fn process_pcm_float(&mut self, pcm: &mut [f32], channels: usize, sample_rate: u32) {
        if pcm.is_empty() || channels == 0 {
            return;
        }
        self.eq.process_interleaved(pcm, channels, sample_rate);
        let gain_mult = if self.normalizer_enabled {
            db_to_linear(self.loudness_gain_db)
        } else {
            1.0
        };
        self.limiter.process_buffer(pcm, gain_mult);
    }

    pub fn process_pcm16(&mut self, pcm: &mut [i16], channels: usize, sample_rate: u32) {
        if pcm.is_empty() || channels == 0 {
            return;
        }
        if self.scratch_f32.len() < pcm.len() {
            self.scratch_f32.resize(pcm.len(), 0.0);
        }
        let buf = &mut self.scratch_f32[..pcm.len()];
        for (dst, &src) in buf.iter_mut().zip(pcm.iter()) {
            *dst = (src as f32) / 32768.0;
        }
        self.eq.process_interleaved(buf, channels, sample_rate);
        let gain_mult = if self.normalizer_enabled {
            db_to_linear(self.loudness_gain_db)
        } else {
            1.0
        };
        self.limiter.process_buffer(buf, gain_mult);
        for (dst, &src) in pcm.iter_mut().zip(buf.iter()) {
            let scaled = (src * 32767.0).clamp(-32768.0, 32767.0);
            *dst = scaled.round() as i16;
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_loudness_gain_calculation() {
        let gain = calculate_loudness_gain_db(Some(7.77)).unwrap();
        assert!((gain - (-7.77)).abs() < 0.01);
        assert_eq!(calculate_loudness_gain_db(Some(0.0)), None);

        let boost = calculate_loudness_gain_db(Some(-1.5)).unwrap();
        assert!((boost - 1.5).abs() < 0.01);

        let capped = calculate_loudness_gain_db(Some(-10.0)).unwrap();
        assert_eq!(capped, MAX_BOOST_DB);

        assert_eq!(calculate_loudness_gain_db(None), Some(-4.5));
    }

    #[test]
    fn test_soft_limiter_bounds() {
        let limiter = SoftLimiter::default();
        assert_eq!(limiter.process_sample(0.5), 0.5);
        assert_eq!(limiter.process_sample(-0.8), -0.8);

        let limited = limiter.process_sample(1.5);
        assert!(limited < 1.0 && limited >= 0.95);

        let limited_neg = limiter.process_sample(-2.0);
        assert!(limited_neg > -1.0 && limited_neg <= -0.95);
    }

    #[test]
    fn test_soft_limiter_smoothness_and_nan_resilience() {
        let limiter = SoftLimiter::default();
        let at_thresh = limiter.process_sample(0.95);
        assert!((at_thresh - 0.95).abs() < 1e-6);

        let just_above = limiter.process_sample(0.9501);
        assert!(just_above >= 0.95 && just_above < 0.9502);

        assert_eq!(calculate_loudness_gain_db(Some(f64::NAN)), Some(-4.5));
        assert_eq!(calculate_loudness_gain_db(Some(f64::INFINITY)), Some(-4.5));
    }

    #[test]
    fn test_equalizer_flat_passthrough() {
        let mut eq = Equalizer::default();
        let mut buffer = vec![0.5, -0.5, 0.25, -0.25];
        let original = buffer.clone();
        eq.process_interleaved(&mut buffer, 2, 48_000);
        assert_eq!(buffer, original);
    }

    #[test]
    fn test_equalizer_boost_and_presets() {
        let mut eq = Equalizer::default();
        let preset = EqPreset::BassPunch;
        eq.set_gains(preset.gains());
        assert!(eq.active);

        let mut buffer = vec![0.1_f32; 100];
        eq.process_interleaved(&mut buffer, 2, 48_000);
        for &s in &buffer {
            assert!(s.is_finite());
        }
    }

    #[test]
    fn test_auto_headroom_and_audio_pipeline() {
        let bass = EqPreset::BassPunch.gains();
        assert!((compute_auto_headroom_db(&bass) - (-6.0)).abs() < 0.01);
        assert_eq!(compute_auto_headroom_db(&EqPreset::Flat.gains()), 0.0);

        let mut pipeline = AudioPipeline::new(48_000);
        pipeline.configure(bass, 0.0, true, 3.0, true);

        let mut pcm16: Vec<i16> = vec![16_000, -16_000, 24_000, -24_000, 32_000, -32_000];
        pipeline.process_pcm16(&mut pcm16, 2, 48_000);
        for &s in &pcm16 {
            assert!(s.abs() <= 32_767);
        }

        let mut pcm_f = vec![0.9_f32, -0.9_f32, 0.5_f32, -0.5_f32];
        pipeline.process_pcm_float(&mut pcm_f, 2, 48_000);
        for &s in &pcm_f {
            assert!(s.is_finite() && s.abs() < 1.0);
        }
    }
}
