package com.thoughtcrimes.securesms.compose_utils.ui

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.thoughtcrimes.securesms.compose_utils.BChatTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BChatPreviewContainer(
    content: @Composable () -> Unit,
) {
    BChatTheme {
        Surface {
            Scaffold {
                content()
            }
        }
    }
}