Task Scheduler ~  Create Task
<br>
<br><strong><i><small>Triggers</small></i></strong>
<br>
<br>
<br><strong><i>Actions</i></strong>
<br>
<br><strong><small>Action 1.</small></strong> 
<br>
<br>Program/script:
<br>        C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
<br>Argument:
<br>        Get-Date | Out-File -FilePath .\batteryreport.txt
<br>Location:
<br>        *Select your desired shared network location that Hubitat can access*
<br>
<br><strong>Action 2. </strong>
<br>
<br>Program/script:
<br>        C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
<br>Argument:
<br>        WMIC PATH Win32_Battery Get EstimatedChargeRemaining | Out-File -FilePath .\batteryreport.txt -Append
<br>Location:
<br>        *Select your desired shared network location that Hubitat can access*