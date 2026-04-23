package app.birdo.vpn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.birdo.vpn.R

/**
 * Renders the actual Birdo launcher icon. Used wherever the app needs to
 * represent itself — login, top bars, profile avatar. Replaces the previous
 * bright purple→pink gradient shield with the real brand mark.
 */
@Composable
fun AppIconMark(
    size: Dp = 40.dp,
    cornerRadius: Dp = 12.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_round),
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Crop,
        )
    }
}
