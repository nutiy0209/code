# 系統功能解說文件

## 1. 用戶資料提交功能
- 用戶可以輸入姓名、性別、年齡和病史，然後提交這些資料到伺服器。
- 使用 `OkHttp` 庫進行資料的 POST 傳輸，目標 URL 是 `http://192.168.53.103/SaveData.php`。

## 2. 餐飲時間提醒
- 用戶可以使用 `TimePicker` 設定早餐、午餐和晚餐的提醒時間。
- 系統使用 `AlarmManager` 來設置提醒，並通過 `AlarmReceiver` 顯示通知。

## 3. 定期提醒功能
- 用戶可以使用 `NumberPicker` 設定喝水、運動和上廁所的間隔時間。
- 系統會根據設定的間隔使用 `AlarmManager` 進行定期提醒。

## 4. 調查問卷提醒
- 用戶可以設定每日的問卷提醒時間。
- 系統每天在指定時間提醒用戶進行問卷調查。

## 使用技術
- Android SDK
- OkHttp：負責網路請求和數據傳輸
- AlarmManager：負責定時提醒
- TimePicker 和 NumberPicker：負責用戶的時間和數字輸入
