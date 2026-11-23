package com.example.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WalletTransaction
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.util.FormatUtils
import com.example.util.ImageUtils

@Composable
fun NeonBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.08f),
                        radius = size.width * 0.75f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricPurple.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.35f),
                        radius = size.width * 0.7f
                    )
                )
            },
        content = content
    )
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    accentBrush: Brush = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.45f), ElectricPurple.copy(alpha = 0.45f))),
    containerColor: Color = ObsidianCard,
    cornerRadius: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, accentBrush),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val upper = status.uppercase()
    val pulseTransition = rememberInfiniteTransition(label = "live_pulse")
    val liveAlpha by pulseTransition.animateColor(
        initialValue = CyberMagenta,
        targetValue = NeonCyan,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "live_color"
    )

    val (bg, fg) = when (upper) {
        "LIVE" -> liveAlpha.copy(alpha = 0.18f) to liveAlpha
        "UPCOMING" -> NeonCyan.copy(alpha = 0.16f) to NeonCyan
        "APPROVED", "COMPLETED" -> NeonEmerald.copy(alpha = 0.16f) to NeonEmerald
        "PENDING" -> AmberGold.copy(alpha = 0.18f) to AmberGold
        "REJECTED", "BLOCKED" -> DangerCrimson.copy(alpha = 0.18f) to DangerCrimson
        "ADMIN", "SUPER_ADMIN" -> ElectricPurple.copy(alpha = 0.22f) to ElectricPurple
        else -> GlassBorder.copy(alpha = 0.4f) to TextSecondary
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (upper == "LIVE") {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
        }
        Text(
            text = upper,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlayerAvatar(
    photoBase64: String?,
    contentDescription: String,
    size: Dp = 48.dp,
    borderColor: Color = NeonCyan
) {
    val bitmap = remember(photoBase64) { ImageUtils.decodeBase64ToImageBitmap(photoBase64) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(ObsidianElevated)
            .border(1.5.dp, borderColor.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_avatar_cyber),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun TournamentBannerImage(
    bannerKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val resId = when (bannerKey) {
        "battle_royale" -> R.drawable.img_banner_battle_royale
        "tactical_fps" -> R.drawable.img_banner_tactical_fps
        else -> R.drawable.img_hero_arena
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun SlotProgressBar(
    filled: Long,
    max: Long,
    modifier: Modifier = Modifier
) {
    val fraction = if (max > 0) (filled.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "slot_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(ObsidianElevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            NeonCyan,
                            if (fraction > 0.85f) CyberMagenta else ElectricPurple
                        )
                    )
                )
        )
    }
}

@Composable
fun TransactionRow(
    tx: WalletTransaction,
    modifier: Modifier = Modifier
) {
    val isCredit = tx.direction == "CREDIT"
    val accent = if (isCredit) NeonEmerald else CyberMagenta
    val prefix = if (isCredit) "+" else "-"
    val icon = if (tx.type == "PRIZE_WIN") {
        Icons.Filled.EmojiEvents
    } else if (isCredit) {
        Icons.AutoMirrored.Filled.TrendingUp
    } else {
        Icons.AutoMirrored.Filled.TrendingDown
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tx.type,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description.ifBlank { tx.type },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = FormatUtils.formatTimestamp(tx.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${FormatUtils.formatPkr(tx.amount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                StatusBadge(status = tx.status)
            }
        }
    }
}

@Composable
fun EmptyArenaState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Filled.SportsEsports,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier.fillMaxWidth(),
        accentBrush = Brush.linearGradient(listOf(GlassBorder, ElectricPurple.copy(alpha = 0.3f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NeonBlue.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
