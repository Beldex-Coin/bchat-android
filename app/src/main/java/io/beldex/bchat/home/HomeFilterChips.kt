package io.beldex.bchat.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors

enum class HomeFilter { All, Social, Groups }

@Composable
fun HomeFilterChips(
    selected: HomeFilter,
    onSelect: (HomeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 8.dp)
    ) {
        HomeFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            val label = when (filter) {
                HomeFilter.All -> stringResource(R.string.home_filter_all)
                HomeFilter.Social -> stringResource(R.string.home_filter_social)
                HomeFilter.Groups -> stringResource(R.string.home_filter_groups)
            }
            val accent = if (isSelected) MaterialTheme.appColors.userDetailsConfirmBackground else MaterialTheme.appColors.homeRowTimestamp
            Text(
                text = label,
                color = accent,
                fontFamily = RobotoMono,
                fontWeight = FontWeight.Normal,
                fontSize = 10.5.sp,
                modifier = Modifier
                    .clickable { onSelect(filter) }
                    .background(if (isSelected) accent.copy(alpha = 0.1f) else Color(0x331A1A1A))
                    .border(0.5.dp, accent)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
