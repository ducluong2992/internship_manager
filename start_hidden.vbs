Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
currentDir = fso.GetParentFolderName(WScript.ScriptFullName)

javaCmd = "javaw.exe"
If fso.FileExists("C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot\bin\javaw.exe") Then
    javaCmd = """C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot\bin\javaw.exe"""
End If

jarPath = """" & currentDir & "\backend\target\qlnv-backend-2.0.0.jar"""
backendDir = currentDir & "\backend"

cmd = javaCmd & " -jar " & jarPath

WshShell.CurrentDirectory = backendDir
WshShell.Run cmd, 0, False

WScript.Echo "Máy chủ đã khởi chạy ngầm tại http://localhost:8000!" & vbCrLf & "Bạn có thể đóng cửa sổ này, chương trình vẫn tiếp tục hoạt động." & vbCrLf & "Để tắt chương trình, vui lòng chạy file stop.bat."
