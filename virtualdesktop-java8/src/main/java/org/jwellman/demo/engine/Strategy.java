package org.jwellman.demo.engine;

public class Strategy {

    // --- LOOPING TIMELINE STRATEGIES ---

    public static class OnceStrategy implements LoopStrategy {
        @Override
        public double calculateProgress(long elapsedMs, long durationMs) {
            if (durationMs <= 0)
                return 1.0;
            return Math.min(1.0, Math.max(0.0, (double) elapsedMs / durationMs));
        }
    }

    // --- EASING CURVE STRATEGIES ---

    public static class EaseInSine implements EasingStrategy {
        @Override
        public double ease(double p) {
            return 1.0 - Math.cos(p * Math.PI / 2.0);
        }
    }

    public static class EaseOutSine implements EasingStrategy {
        @Override
        public double ease(double p) {
            return Math.sin(p * Math.PI / 2.0);
        }
    }

    public static class EaseInOutSine implements EasingStrategy {
        @Override
        public double ease(double p) {
            return -(Math.cos(Math.PI * p) - 1.0) / 2.0;
        }
    }

    public static class EaseInCubic implements EasingStrategy {
        @Override
        public double ease(double p) {
            return p * p * p;
        }
    }

    public static class EaseOutCubic implements EasingStrategy {
        @Override
        public double ease(double p) {
            return 1.0 - Math.pow(1.0 - p, 3);
        }
    }

    public static class EaseInOutCubic implements EasingStrategy {
        @Override
        public double ease(double p) {
            return p < 0.5 ? 4.0 * p * p * p : 1.0 - Math.pow(-2.0 * p + 2.0, 3) / 2.0;
        }
    }

    public static class EaseInQuint implements EasingStrategy {
        @Override
        public double ease(double p) {
            return p * p * p * p * p;
        }
    }

    public static class EaseOutQuint implements EasingStrategy {
        @Override
        public double ease(double p) {
            return 1.0 - Math.pow(1.0 - p, 5);
        }
    }

    public static class EaseInOutQuint implements EasingStrategy {
        @Override
        public double ease(double p) {
            return p < 0.5 ? 16.0 * p * p * p * p * p : 1.0 - Math.pow(-2.0 * p + 2.0, 5) / 2.0;
        }
    }

}
