package ca.uwaterloo.cs346project

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class DrawnItem(
    // pair of user id and a user-specific object id
    val userObjectId: Pair<Int, Int> = Pair(-1, 0),
    val shape: Shape = Shape.Line,
    var color: Color = Color.Black,
    val strokeWidth: Float = 4f,
    val segmentPoints: SnapshotStateList<Offset> = mutableStateListOf()
) {
    companion object {
        var maxUserObjectId = 0
        fun newUserObjectId(): Pair<Int, Int> {
            val userObjectId = maxUserObjectId
            maxUserObjectId++
            return Pair(client.user_id, userObjectId)
        }
    }
}


// Contains attributes for pen and eraser as well as the drawing mode
data class DrawInfo (
    val drawMode: DrawMode = DrawMode.Pen,
    val shape: Shape = Shape.Line,
    val color: Color = Color.Black,
    val strokeWidth: Float = 4f,
)

@Serializable
data class Action<T>(
    val type: ActionType,
    // MODIFY: items stores 2 (only 2) DrawnItem objects, [oldState, newState] (order preserved)
    // REMOVE: items stores the DrawnItem objects to be removed from drawnItems array
    // ADD: items stores a list of DrawnItem objects to be added to drawnItems array
    val items: List<T>, // The items involved in the action
)

@Serializable
enum class ActionType(val value: UInt) {
    ADD(0U), // When a new item is added
    REMOVE(1U), // When an item is removed (e.g., erasing)
    MODIFY(2U); // When an item is modified (e.g., moved or resized)
}


const val MAX_STROKE_WIDTH = 140f

enum class DrawMode { Pen, Eraser, Shape, CanvasDrag, Selection }

enum class ToolbarExtensionSetting { ColorSelection, StrokeWidthAdjustment, ShapeSelection, Hidden }

enum class CurrentPage {HomePage, WhiteboardPage}
data class Pg (
    var curPage : CurrentPage = CurrentPage.HomePage
)

@Serializable
enum class Shape(val value: UInt) {
    Rectangle(0U),
    Oval(1U),
    Line(2U),
    StraightLine(3U);
}



// Checks if a point is close enough to a rectangle/oval
fun isPointCloseToRectangle(point: Offset, item: DrawnItem): Boolean {
    if (item.shape != Shape.Rectangle && item.shape != Shape.Oval) {
        return false
    }
    if (item.segmentPoints.size < 2) {
        return false
    }

    val start = item.segmentPoints[0]
    val end = item.segmentPoints[1]

    val margin = 100f
    val minX = min(start.x, end.x)
    val maxX = max(start.x, end.x)
    val minY = min(start.y, end.y)
    val maxY = max(start.y, end.y)
    return point.x in (minX - margin)..(maxX + margin) && point.y in (minY - margin)..(maxY + margin) &&
            !(point.x in (minX + margin)..(maxX - margin) && point.y in (minY + margin)..(maxY - margin))
}



// Checks if a point is close enough to a straight line
fun isPointCloseToLine(point: Offset, item: DrawnItem): Boolean {
    if (item.shape != Shape.StraightLine) {
        return false
    }
    if (item.segmentPoints.size < 2) {
        return false
    }
    val threshold = item.strokeWidth + 50f

    val start = item.segmentPoints[0]
    val end = item.segmentPoints[1]

    val x1 = start.x
    val y1 = start.y
    val x2 = end.x
    val y2 = end.y
    val px = point.x
    val py = point.y

    val dx = x2 - x1
    val dy = y2 - y1
    val l2 = dx * dx + dy * dy
    if (l2 == 0f) { // Line is a point
        // Check if the point is within the threshold distance from the start (which is also the end)
        val distance = sqrt((start.x - point.x) * (start.x - point.x) + (start.y - point.y) * (start.y - point.y))
        return distance < threshold
    }

    val t = ((px - x1) * dx + (py - y1) * dy) / l2
    val tt = max(0f, min(1f, t))
    val projX = x1 + tt * dx
    val projY = y1 + tt * dy
    val distance = sqrt((projX - px) * (projX - px) + (projY - py) * (projY - py))

    return distance < threshold
}