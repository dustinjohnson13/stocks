package com.jdom.util

/**
 * Created by djohnson on 11/15/14.
 */
class TimeUtilHelper {

    private static class FrozenClock implements TimeUtil.Clock {

        private long frozenTime

        private FrozenClock(long time) {
            this.frozenTime = time
        }

        @Override
        long currentTimeMillis() {
            return frozenTime
        }
    }

    static void freezeTime(Date date = new Date()) {
        TimeUtil.clock = new FrozenClock(date.getTime())
    }

    static void resumeTime() {
        TimeUtil.clock = new TimeUtil.WallClock()
    }

}
