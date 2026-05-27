package de.hbch.traewelling.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.hbch.traewelling.R
import de.hbch.traewelling.api.models.alert.Alert
import de.hbch.traewelling.theme.LocalColorScheme
import de.hbch.traewelling.theme.LocalFont
import de.hbch.traewelling.ui.composables.ButtonWithIconAndText
import de.hbch.traewelling.util.openLink
import java.util.Locale
import androidx.core.net.toUri

@Composable
fun AlertCarousel(
    traewellingAlerts: List<Alert>,
    modifier: Modifier = Modifier
) {
    if (traewellingAlerts.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { traewellingAlerts.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            AlertCard(
                alert = traewellingAlerts[page],
                traewellingAlert = true
            )
        }

        if (traewellingAlerts.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(traewellingAlerts.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        LocalColorScheme.current.primary
                    } else {
                        LocalColorScheme.current.primary.copy(alpha = 0.2f)
                    }
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AlertCard(
    alert: Alert,
    traewellingAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLocale = Locale.getDefault().language
    val translation = alert.translations?.find { it.locale == currentLocale }
        ?: alert.translations?.find { it.locale == "en" }
        ?: alert.translations?.firstOrNull()

    if (translation == null) return

    OutlinedCard (
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(alert.type.icon),
                    contentDescription = null,
                    tint = alert.type.color ?: Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = translation.title,
                    style = LocalFont.current.titleMedium,
                    color = alert.type.color ?: Color.Unspecified,
                    modifier = Modifier.weight(1f)
                )
            }
            var msg = ""
            if (traewellingAlert) {
                msg += stringResource(R.string.message_from, stringResource(R.string.traewelling))
                if (currentLocale != translation.locale) {
                    msg += " – ${stringResource(R.string.displayed_in_account_language)}"
                }
            }
            Text(
                text = msg,
                style = LocalFont.current.labelSmall,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth()
            )
            if (translation.content.isNotBlank()) {
                Text(
                    text = translation.content,
                    style = LocalFont.current.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }
            val url = translation.url.ifBlank { alert.url }
            if (!url.isNullOrBlank()) {
                val uri = url.toUri()
                ButtonWithIconAndText(
                    text = uri.host ?: url,
                    drawableId = R.drawable.ic_arrow_right,
                    onClick = {
                        context.openLink(url)
                    }
                )
            }
        }
    }
}
