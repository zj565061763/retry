package com.sd.demo.retry

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sd.lib.retry.FRetry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class RetryTest {

    @Test
    fun testRetryNormal() {
        val events = mutableListOf<String>()
        val retry = TestRetry(events = events)

        retry.startRetry()
        assertEquals(FRetry.State.Running, retry.state)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(FRetry.State.Idle, retry.state)

        assertEquals("onStart|checkRetry|onRetry|onStop", events.joinToString("|"))
    }

    @Test
    fun testRetryPauseResume() {
        val events = mutableListOf<String>()
        val checkRetryFlag = AtomicBoolean(false)

        val retry = TestRetry(
            events = events,
            checkRetry = { checkRetryFlag.get() }
        )

        retry.startRetry()
        assertEquals(FRetry.State.Running, retry.state)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(FRetry.State.Paused, retry.state)

        checkRetryFlag.set(true)
        retry.tryResumeRetry()
        assertEquals(FRetry.State.Running, retry.state)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(FRetry.State.Idle, retry.state)

        assertEquals("onStart|checkRetry|onPause|checkRetry|onRetry|onStop", events.joinToString("|"))
    }

    @Test
    fun testRetryReturnFalse() {
        val events = mutableListOf<String>()

        val retry = TestRetry(
            events = events,
            onRetry = {
                it.retry()
                false
            },
        )

        retry.startRetry()
        retry.waitForIdle()
        assertEquals("onStart|checkRetry|onRetry|onStop", events.joinToString("|"))
    }

    @Test
    fun testRetryCount() {
        val events = mutableListOf<String>()

        val retry = TestRetry(
            maxRetryCount = 2,
            events = events,
            onRetry = {
                it.retry()
                true
            },
        )

        retry.setRetryInterval(100)
        retry.startRetry()
        retry.waitForIdle()
        assertEquals("onStart|checkRetry|onRetry|checkRetry|onRetry|onStop|onRetryMaxCount", events.joinToString("|"))
    }

    @Test
    fun testRetrySessionFinish() {
        val events = mutableListOf<String>()

        val retry = TestRetry(
            maxRetryCount = 2,
            events = events,
            onRetry = {
                if (retryCount == 1) {
                    it.retry()
                } else {
                    it.finish()
                }
                true
            },
        )

        retry.setRetryInterval(100)
        retry.startRetry()
        retry.waitForIdle()
        assertEquals("onStart|checkRetry|onRetry|checkRetry|onRetry|onStop", events.joinToString("|"))
    }
}

private class TestRetry(
    maxRetryCount: Int = Int.MAX_VALUE,
    private val events: MutableList<String> = mutableListOf(),
    private val checkRetry: TestRetry.() -> Boolean = { true },
    private val onStart: TestRetry.() -> Unit = {},
    private val onPause: TestRetry.() -> Unit = {},
    private val onStop: TestRetry.() -> Unit = {},
    private val onRetryMaxCount: TestRetry.() -> Unit = {},
    private val onRetry: TestRetry.(Session) -> Boolean = { false },
) : FRetry(maxRetryCount = maxRetryCount) {

    override fun checkRetry(): Boolean {
        checkMainLooper()
        events.add("checkRetry")
        return checkRetry.invoke(this)
    }

    override fun onStart() {
        checkMainLooper()
        events.add("onStart")
        onStart.invoke(this)
    }

    override fun onPause() {
        checkMainLooper()
        events.add("onPause")
        onPause.invoke(this)
    }

    override fun onStop() {
        checkMainLooper()
        events.add("onStop")
        onStop.invoke(this)
    }

    override fun onRetry(session: Session): Boolean {
        checkMainLooper()
        events.add("onRetry")
        return onRetry.invoke(this, session)
    }

    override fun onRetryMaxCount() {
        checkMainLooper()
        events.add("onRetryMaxCount")
        onRetryMaxCount.invoke(this)
    }

    fun tryResumeRetry() {
        super.resumeRetry()
    }

    fun waitForIdle() {
        while (true) {
            if (state == State.Idle) {
                break
            } else {
                Thread.sleep(10)
                continue
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}

private fun checkMainLooper() {
    check(Looper.myLooper() === Looper.getMainLooper())
}