package com.sd.demo.retry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.demo.retry.theme.AppTheme
import com.sd.lib.retry.FNetRetry
import com.sd.lib.retry.FRetry

class SampleRetry : ComponentActivity() {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         AppTheme {
            ContentView(
               onClickStart = {
                  FRetry.start(AppRetry::class.java)
               },
               onClickCancel = {
                  FRetry.stop(AppRetry::class.java)
               },
            )
         }
      }
   }

   override fun onDestroy() {
      super.onDestroy()
      FRetry.stop(AppRetry::class.java)
   }
}

@Composable
private fun ContentView(
   modifier: Modifier = Modifier,
   onClickStart: () -> Unit,
   onClickCancel: () -> Unit,
) {
   Column(
      modifier = modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
   ) {
      Button(onClick = onClickStart) {
         Text(text = "Start")
      }

      Button(onClick = onClickCancel) {
         Text(text = "Cancel")
      }
   }
}

internal class AppRetry : FNetRetry(15) {

   init {
      setRetryInterval(1000)
   }

   override fun onStart() {
      super.onStart()
      logMsg { "$this onStart" }
   }

   override fun onPause() {
      super.onPause()
      logMsg { "$this onPause" }
   }

   override fun onStop() {
      super.onStop()
      logMsg { "$this onStop" }
   }

   override fun onRetry(session: Session): Boolean {
      logMsg { "$this onRetry $retryCount" }

      if (retryCount >= 10) {
         session.finish()
      } else {
         session.retry()
      }

      return true
   }

   override fun onRetryMaxCount() {
      super.onRetryMaxCount()
      logMsg { "$this onRetryMaxCount" }
   }
}