package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ComposeIcons {
    val Error: ImageVector
        get() {
            if (_Error != null) {
                return _Error!!
            }
            _Error = ImageVector.Builder(
                name = "Error",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(480f, 680f)
                    quadToRelative(17f, 0f, 28.5f, -11.5f)
                    reflectiveQuadTo(520f, 640f)
                    reflectiveQuadToRelative(-11.5f, -28.5f)
                    reflectiveQuadTo(480f, 600f)
                    reflectiveQuadToRelative(-28.5f, 11.5f)
                    reflectiveQuadTo(440f, 640f)
                    reflectiveQuadToRelative(11.5f, 28.5f)
                    reflectiveQuadTo(480f, 680f)
                    moveToRelative(-40f, -160f)
                    horizontalLineToRelative(80f)
                    verticalLineToRelative(-240f)
                    horizontalLineToRelative(-80f)
                    close()
                    moveToRelative(40f, 360f)
                    quadToRelative(-83f, 0f, -156f, -31.5f)
                    reflectiveQuadTo(197f, 763f)
                    reflectiveQuadToRelative(-85.5f, -127f)
                    reflectiveQuadTo(80f, 480f)
                    reflectiveQuadToRelative(31.5f, -156f)
                    reflectiveQuadTo(197f, 197f)
                    reflectiveQuadToRelative(127f, -85.5f)
                    reflectiveQuadTo(480f, 80f)
                    reflectiveQuadToRelative(156f, 31.5f)
                    reflectiveQuadTo(763f, 197f)
                    reflectiveQuadToRelative(85.5f, 127f)
                    reflectiveQuadTo(880f, 480f)
                    reflectiveQuadToRelative(-31.5f, 156f)
                    reflectiveQuadTo(763f, 763f)
                    reflectiveQuadToRelative(-127f, 85.5f)
                    reflectiveQuadTo(480f, 880f)
                    moveToRelative(0f, -80f)
                    quadToRelative(134f, 0f, 227f, -93f)
                    reflectiveQuadToRelative(93f, -227f)
                    reflectiveQuadToRelative(-93f, -227f)
                    reflectiveQuadToRelative(-227f, -93f)
                    reflectiveQuadToRelative(-227f, 93f)
                    reflectiveQuadToRelative(-93f, 227f)
                    reflectiveQuadToRelative(93f, 227f)
                    reflectiveQuadToRelative(227f, 93f)
                    moveToRelative(0f, -320f)
                }
            }.build()
            return _Error!!
        }

    private var _Error: ImageVector? = null

    val Folder: ImageVector
        get() {
            if (_Folder != null) {
                return _Folder!!
            }
            _Folder = ImageVector.Builder(
                name = "Folder",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    fillAlpha = 1.0f,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(20f, 20f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, -2f)
                    verticalLineTo(8f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, -2f)
                    horizontalLineToRelative(-7.9f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.69f, -0.9f)
                    lineTo(9.6f, 3.9f)
                    arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7.93f, 3f)
                    horizontalLineTo(4f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, 2f)
                    verticalLineToRelative(13f)
                    arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 2f)
                    close()
                }
            }.build()
            return _Folder!!
        }

    private var _Folder: ImageVector? = null

    val Info: ImageVector
        get() {
            if (_Info != null) {
                return _Info!!
            }
            _Info = ImageVector.Builder(
                name = "Info",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = null,
                    fillAlpha = 1.0f,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(22f, 12f)
                    arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 22f)
                    arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 12f)
                    arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 22f, 12f)
                    close()
                }
                path(
                    fill = null,
                    fillAlpha = 1.0f,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12f, 16f)
                    verticalLineToRelative(-4f)
                }
                path(
                    fill = null,
                    fillAlpha = 1.0f,
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12f, 8f)
                    horizontalLineToRelative(0.01f)
                }
            }.build()
            return _Info!!
        }

    private var _Info: ImageVector? = null

    val MoreVert: ImageVector
        get() {
            if (_MoreVert != null) {
                return _MoreVert!!
            }
            _MoreVert = ImageVector.Builder(
                name = "More_vert",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(480f, 800f)
                    quadToRelative(-33f, 0f, -56.5f, -23.5f)
                    reflectiveQuadTo(400f, 720f)
                    reflectiveQuadToRelative(23.5f, -56.5f)
                    reflectiveQuadTo(480f, 640f)
                    reflectiveQuadToRelative(56.5f, 23.5f)
                    reflectiveQuadTo(560f, 720f)
                    reflectiveQuadToRelative(-23.5f, 56.5f)
                    reflectiveQuadTo(480f, 800f)
                    moveToRelative(0f, -240f)
                    quadToRelative(-33f, 0f, -56.5f, -23.5f)
                    reflectiveQuadTo(400f, 480f)
                    reflectiveQuadToRelative(23.5f, -56.5f)
                    reflectiveQuadTo(480f, 400f)
                    reflectiveQuadToRelative(56.5f, 23.5f)
                    reflectiveQuadTo(560f, 480f)
                    reflectiveQuadToRelative(-23.5f, 56.5f)
                    reflectiveQuadTo(480f, 560f)
                    moveToRelative(0f, -240f)
                    quadToRelative(-33f, 0f, -56.5f, -23.5f)
                    reflectiveQuadTo(400f, 240f)
                    reflectiveQuadToRelative(23.5f, -56.5f)
                    reflectiveQuadTo(480f, 160f)
                    reflectiveQuadToRelative(56.5f, 23.5f)
                    reflectiveQuadTo(560f, 240f)
                    reflectiveQuadToRelative(-23.5f, 56.5f)
                    reflectiveQuadTo(480f, 320f)
                }
            }.build()
            return _MoreVert!!
        }

    private var _MoreVert: ImageVector? = null

}
