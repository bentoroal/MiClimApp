package cl.bentoroal.appcuidadodeplantas.model

data class ScheduledNotification(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val repeatDays: List<Int>, // Calendar.DAY_OF_WEEK: 1 = Sunday, 7 = Saturday
    val message: String
)