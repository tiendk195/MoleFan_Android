package com.molefan.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object I18n {
    var lang by mutableStateOf("vi")
    private val T: Map<String, Array<String>> = mapOf(
        "tab.overview" to arrayOf("Tổng quan", "Overview", "总览"),
        "tab.thermal" to arrayOf("Nhiệt độ", "Thermal", "温度"),
        "tab.clean" to arrayOf("Dọn dẹp", "Clean", "清理"),
        "tab.analyze" to arrayOf("Phân tích", "Analyze", "分析"),
        "tab.apps" to arrayOf("Ứng dụng", "Apps", "应用"),
        "tab.optimize" to arrayOf("Tối ưu", "Optimize", "优化"),
        "health" to arrayOf("Sức khoẻ máy", "Health", "健康度"),
        "good" to arrayOf("Tốt", "Good", "良好"),
        "fair" to arrayOf("Trung bình", "Fair", "一般"),
        "poor" to arrayOf("Kém", "Poor", "较差"),
        "cpu" to arrayOf("CPU", "CPU", "CPU"),
        "ram" to arrayOf("Bộ nhớ RAM", "Memory", "内存"),
        "storage" to arrayOf("Bộ nhớ trong", "Storage", "存储"),
        "battery" to arrayOf("Pin", "Battery", "电池"),
        "network" to arrayOf("Mạng", "Network", "网络"),
        "uptime" to arrayOf("Thời gian chạy", "Uptime", "运行时间"),
        "free" to arrayOf("Còn trống %s", "%s free", "剩余 %s"),
        "charging" to arrayOf("Đang sạc", "Charging", "充电中"),
        "discharging" to arrayOf("Đang dùng", "On battery", "放电中"),
        "cycles" to arrayOf("%d chu kỳ", "%d cycles", "%d 次循环"),
        "na" to arrayOf("Không đọc được", "Unavailable", "无法读取"),
        "cpu.note" to arrayOf("Android chặn đọc /proc/stat trên một số máy; khi đó hiển thị theo xung nhịp.", "Android may block /proc/stat; falling back to clock speed.", "部分设备禁止读取 /proc/stat，改用频率估算。"),
        "thermal.status" to arrayOf("Trạng thái nhiệt hệ thống", "System thermal status", "系统温度状态"),
        "th.0" to arrayOf("Bình thường", "Normal", "正常"),
        "th.1" to arrayOf("Hơi ấm", "Light", "轻微"),
        "th.2" to arrayOf("Vừa", "Moderate", "中等"),
        "th.3" to arrayOf("Nặng", "Severe", "严重"),
        "th.4" to arrayOf("Nguy kịch", "Critical", "危急"),
        "th.5" to arrayOf("Khẩn cấp", "Emergency", "紧急"),
        "th.6" to arrayOf("Tắt máy", "Shutdown", "关机"),
        "headroom" to arrayOf("Dự trữ nhiệt (10s tới)", "Thermal headroom (10s)", "温度余量(10秒)"),
        "sensors" to arrayOf("Cảm biến nhiệt", "Thermal sensors", "温度传感器"),
        "sensors.none" to arrayOf("Android không cho app đọc cảm biến /sys; chỉ có nhiệt độ pin.", "Android blocks /sys sensors for apps; only battery temp is available.", "系统不允许应用读取 /sys 传感器，仅电池温度可用。"),
        "history" to arrayOf("Lịch sử nhiệt độ pin", "Battery temp history", "电池温度历史"),
        "fan.note" to arrayOf("Máy không có quạt điều khiển được bằng app. Thay bằng bài test tải nhiệt.", "No app-controllable fan on this phone. Use the thermal stress test instead.", "手机无可控风扇，改用温度压力测试。"),
        "stress" to arrayOf("Test tải nhiệt CPU", "CPU thermal stress test", "CPU 温度压力测试"),
        "stress.start" to arrayOf("Bắt đầu (60s)", "Start (60s)", "开始 (60秒)"),
        "stress.stop" to arrayOf("Dừng", "Stop", "停止"),
        "stress.run" to arrayOf("Đang chạy %d/60s · pin %.1f°C", "Running %d/60s · batt %.1f°C", "运行中 %d/60秒 · 电池 %.1f°C"),
        "stress.res" to arrayOf("Nhiệt pin %.1f → %.1f°C · mức nhiệt cao nhất: %s", "Battery %.1f → %.1f°C · peak status: %s", "电池 %.1f → %.1f°C · 最高状态: %s"),
        "pass" to arrayOf("ĐẠT", "PASS", "通过"),
        "fail" to arrayOf("KHÔNG ĐẠT (máy bị giảm xung)", "FAIL (throttled)", "未通过 (已降频)"),
        "perm.title" to arrayOf("Cần quyền truy cập mọi tệp", "All files access needed", "需要所有文件访问权限"),
        "perm.btn" to arrayOf("Cấp quyền", "Grant", "授权"),
        "scan" to arrayOf("Quét", "Scan", "扫描"),
        "scanning" to arrayOf("Đang quét…", "Scanning…", "扫描中…"),
        "clean.sel" to arrayOf("Dọn mục đã chọn", "Clean selected", "清理所选"),
        "clean.done" to arrayOf("Đã dọn %s", "Cleaned %s", "已清理 %s"),
        "j.cache" to arrayOf("Cache của MoleFan", "MoleFan cache", "MoleFan 缓存"),
        "j.temp" to arrayOf("Tệp tạm & log", "Temp & log files", "临时文件和日志"),
        "j.thumb" to arrayOf("Ảnh thu nhỏ (.thumbnails)", "Thumbnails", "缩略图"),
        "j.apk" to arrayOf("APK đã tải", "Downloaded APKs", "已下载 APK"),
        "trash" to arrayOf("Thùng rác MoleFan", "MoleFan Trash", "MoleFan 回收站"),
        "trash.restore" to arrayOf("Khôi phục", "Restore", "恢复"),
        "trash.empty" to arrayOf("Xoá hẳn", "Empty", "清空"),
        "trash.note" to arrayOf("Mục dọn được chuyển vào thùng rác, khôi phục được.", "Items go to trash and can be restored.", "清理项移入回收站，可恢复。"),
        "items" to arrayOf("%d mục", "%d items", "%d 项"),
        "analyze.up" to arrayOf("↑ Lên thư mục cha", "↑ Parent folder", "↑ 上级目录"),
        "delete" to arrayOf("Chuyển vào thùng rác", "Move to trash", "移入回收站"),
        "cancel" to arrayOf("Huỷ", "Cancel", "取消"),
        "ok" to arrayOf("Đồng ý", "OK", "确定"),
        "confirm.del" to arrayOf("Chuyển \"%s\" vào thùng rác?", "Move \"%s\" to trash?", "将“%s”移入回收站？"),
        "search" to arrayOf("Tìm ứng dụng", "Search apps", "搜索应用"),
        "uninstall" to arrayOf("Gỡ", "Uninstall", "卸载"),
        "apps.note" to arrayOf("Sắp theo dung lượng APK. Android không cho app tự gỡ ngầm: sẽ hiện hộp thoại xác nhận.", "Sorted by APK size. Android asks you to confirm each uninstall.", "按 APK 大小排序，卸载需系统确认。"),
        "opt.cache" to arrayOf("Xoá cache MoleFan", "Clear MoleFan cache", "清除 MoleFan 缓存"),
        "opt.gc" to arrayOf("Giải phóng bộ nhớ app", "Free app memory", "释放应用内存"),
        "opt.storage" to arrayOf("Mở cài đặt bộ nhớ", "Open storage settings", "打开存储设置"),
        "opt.battery" to arrayOf("Tối ưu pin theo app", "Per-app battery optimisation", "应用电池优化"),
        "opt.apps" to arrayOf("Quản lý ứng dụng chạy nền", "Manage background apps", "管理后台应用"),
        "opt.dev" to arrayOf("Mở tuỳ chọn nhà phát triển", "Open developer options", "打开开发者选项"),
        "opt.done" to arrayOf("Đã xong", "Done", "已完成"),
        "opt.note" to arrayOf("Android tự quản lý RAM; các mục dưới mở thẳng trang cài đặt hệ thống tương ứng.", "Android manages RAM itself; items below open the matching system settings.", "Android 自行管理内存，以下项目直接打开系统设置。"),
        "lang" to arrayOf("Ngôn ngữ", "Language", "语言"),
        "theme" to arrayOf("Giao diện", "Theme", "主题"),
    )
    fun tr(k: String): String {
        val a = T[k] ?: return k
        return a[when (lang) { "en" -> 1; "zh" -> 2; else -> 0 }]
    }
}

fun tr(k: String): String = I18n.tr(k)
fun tr(k: String, vararg a: Any): String = String.format(I18n.tr(k), *a)
