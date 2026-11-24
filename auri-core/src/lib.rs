pub mod dsp;
pub mod model;
pub mod ytm;

#[macro_export]
macro_rules! log_info {
    ($target:expr, $($arg:tt)*) => {
        #[cfg(debug_assertions)]
        eprintln!("[INFO][{}] {}", $target, format_args!($($arg)*));
    };
}

#[macro_export]
macro_rules! log_warn {
    ($target:expr, $($arg:tt)*) => {
        eprintln!("[WARN][{}] {}", $target, format_args!($($arg)*));
    };
}

#[macro_export]
macro_rules! log_error {
    ($target:expr, $($arg:tt)*) => {
        eprintln!("[ERROR][{}] {}", $target, format_args!($($arg)*));
    };
}

#[macro_export]
macro_rules! log_stream {
    ($target:expr, $($arg:tt)*) => {
        #[cfg(debug_assertions)]
        eprintln!("[STREAM][{}] {}", $target, format_args!($($arg)*));
    };
}

pub mod auth {
    pub const ORIGIN: &str = "https://music.youtube.com";
    pub fn authorization(_cookie: &str, _now: u64) -> Option<String> {
        None
    }
}
