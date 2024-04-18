package com.sd.demo.retry

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sd.lib.retry.FRetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class RetryTest {

    @Test
    fun testRetryNormal() {
        val retry = TestRetry()

        retry.startRetry()
        assertEquals(FRetry.State.Running, retry.state)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(FRetry.State.Idle, retry.state)
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))

        retry.events.clear()
        retry.startRetry()
        retry.startRetry()
        retry.startRetry()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testRetryPauseResume() {
        val checkRetryFlag = AtomicBoolean(false)

        val retry = TestRetry(
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

        assertEquals("onStart|checkRetry|onPause|checkRetry|onRetry|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testRetryReturnFalse() {
        val retry = TestRetry(
            onRetry = {
                it.retry()
                false
            },
        )

        retry.startRetry()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testRetryCount() {
        val retry = TestRetry(
            maxRetryCount = 2,
            onRetry = {
                it.retry()
                true
            },
        )

        retry.setRetryInterval(100)
        retry.startRetry()
        retry.waitForIdle()
        assertEquals("onStart|checkRetry|onRetry|checkRetry|onRetry|onStop|onRetryMaxCount", retry.events.joinToString("|"))
    }

    @Test
    fun testRetrySession() {
        val retry = TestRetry(
            maxRetryCount = 2,
            onRetry = {
                if (retryCount == 1) {
                    it.retry()
                    it.retry()
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
        assertEquals("onStart|checkRetry|onRetry|checkRetry|onRetry|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testRetrySessionLifecycleRetry() {
        var session: FRetry.Session? = null

        val retry = TestRetry(
            onRetry = {
                if (session == null) {
                    session = it
                }
                false
            },
        )

        retry.startRetry()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))

        session!!.retry()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testRetrySessionLifecycleFinish() {
        var session: FRetry.Session? = null

        val retry = TestRetry(
            onRetry = {
                if (session == null) {
                    session = it
                }
                false
            },
        )

        retry.startRetry()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))

        retry.events.clear()
        retry.startRetry()
        session!!.finish()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testCancelAfterStart() {
        val retry = TestRetry()

        retry.startRetry()
        retry.stopRetry()

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testCancelOnStart() {
        val retry = TestRetry(
            onStart = { stopRetry() },
        )

        retry.startRetry()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testCancelOnPause() {
        val retry = TestRetry(
            checkRetry = { false },
            onPause = { stopRetry() },
        )

        retry.startRetry()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|checkRetry|onPause|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testCancelOnRetry() {
        val retry = TestRetry(
            onRetry = {
                stopRetry()
                true
            },
        )

        retry.startRetry()
        retry.waitForIdle()
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))
    }

    @Test
    fun testGlobalRetry() {
        val retry = FRetry.start(TestRetry::class.java)
        FRetry.start(TestRetry::class.java).also {
            assertTrue(it === retry)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|checkRetry|onRetry|onStop", retry.events.joinToString("|"))

        retry.events.clear()
        FRetry.start(TestRetry::class.java)
        FRetry.stop(TestRetry::class.java)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("onStart|onStop", retry.events.joinToString("|"))
    }
}

class TestRetry(
    maxRetryCount: Int = Int.MAX_VALUE,
    val events: MutableList<String> = mutableListOf(),
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