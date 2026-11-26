package com.auri.music.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletContainer

data class FloatingNavItem(
    val id: Int,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val FloatingBarHeight = 60.dp
private val FloatingItemHeight = 44.dp
private val FloatingBarShadow = 8.dp

@Composable
fun MetrolistFloatingNavigation(
    items: List<FloatingNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    separateSearchItem: FloatingNavItem? = null,
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Main pill container with glass border and elevated surface
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .height(FloatingBarHeight)
                .border(1.dp, AuriGlassBorder, CircleShape),
            shape = CircleShape,
            color = AuriGlassSurfaceElevated.copy(alpha = 0.92f),
            shadowElevation = FloatingBarShadow,
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val selected = item.id == selectedIndex
                    FloatingNavigationTab(
                        item = item,
                        selected = selected,
                        onClick = { onSelect(item.id) },
                    )
                }
            }
        }

        // Separate action pill (e.g., Search or Settings) if specified
        if (separateSearchItem != null) {
            val selected = separateSearchItem.id == selectedIndex
            val containerColor by animateColorAsState(
                if (selected) AuriVioletContainer else AuriGlassSurfaceElevated.copy(alpha = 0.92f),
                label = "floatingSearchContainer",
            )
            val iconTint by animateColorAsState(
                if (selected) AuriVioletBright else AuriTextMuted,
                label = "floatingSearchTint",
            )
            Surface(
                onClick = { onSelect(separateSearchItem.id) },
                modifier = Modifier
                    .size(FloatingBarHeight)
                    .border(
                        1.dp,
                        if (selected) AuriVioletBright.copy(alpha = 0.5f) else AuriGlassBorder,
                        CircleShape
                    ),
                shape = CircleShape,
                color = containerColor,
                shadowElevation = FloatingBarShadow,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (selected) separateSearchItem.selectedIcon else separateSearchItem.icon,
                        contentDescription = separateSearchItem.label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavigationTab(
    item: FloatingNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) AuriVioletContainer else AuriGlassSurfaceElevated.copy(alpha = 0f),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
        label = "floatingItemIndicator",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) AuriVioletBright else AuriTextMuted,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
        label = "floatingItemContent",
    )

    Row(
        modifier = modifier
            .height(FloatingItemHeight)
            .clip(CircleShape)
            .background(indicatorColor)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = AuriVioletBright),
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(spring(dampingRatio = 0.9f, stiffness = 700f)),
            exit = fadeOut() + shrinkHorizontally(spring(dampingRatio = 0.9f, stiffness = 700f)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.label,
                    color = AuriTextPrimary,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
