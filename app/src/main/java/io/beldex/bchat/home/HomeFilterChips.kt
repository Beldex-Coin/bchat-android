package io.beldex.bchat.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        HomeFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            val label = when (filter) {
                HomeFilter.All -> stringResource(R.string.home_filter_all)
                HomeFilter.Social -> stringResource(R.string.home_filter_social)
                HomeFilter.Groups -> stringResource(R.string.home_filter_groups)
            }
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.appColors.userDetailsConfirmBackground else MaterialTheme.appColors.homeRowTimestamp,
                fontFamily = RobotoMono,
                fontWeight = FontWeight.Normal,
                fontSize = 10.5.sp,
                modifier = Modifier
                    .background(
                        if (isSelected) Color.Transparent else MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground,
                        RoundedCornerShape(4.dp)
                    )
                    .then(
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    .clickable { onSelect(filter) }
            )
        }
    }
}
