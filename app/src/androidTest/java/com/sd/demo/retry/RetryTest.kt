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
    fun testState() {
        TestRetry { false }.let { retry ->
            retry.startRetry()
            assertEquals(FRetry.State.Running, retry.state)

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(FRetry.State.Idle, retry.state)
        }

        val checkRetryFlag = AtomicBoolean(false)

        TestRetry(
            checkRetry = { checkRetryFlag.get() },
        ) { false }.let { retry ->
            retry.startRetry()
            assertEquals(FRetry.State.Running, retry.state)

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(FRetry.State.Paused, retry.state)

            checkRetryFlag.set(true)
            retry.tryResumeRetry()
            assertEquals(FRetry.State.Running, retry.state)

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(FRetry.State.Idle, retry.state)
        }
    }
}

private class TestRetry(
    maxRetryCount: Int = Int.MAX_VALUE,
    private val checkRetry: () -> Boolean = { true },
    private val onRetry: (Session) -> Boolean,
) : FRetry(maxRetryCount = maxRetryCount) {

    override fun checkRetry(): Boolean {
        checkMainLooper()
        return checkRetry.invoke()
    }

    override fun onRetry(session: Session): Boolean {
        checkMainLooper()
        return onRetry.invoke(session)
    }

    fun tryResumeRetry() {
        super.resumeRetry()
    }
}

private fun checkMainLooper() {
    check(Looper.myLooper() === Looper.getMainLooper())
}