Task Scheduler ~  Create Task
<br>
<br><strong><i><small>Triggers</small></i></strong>
<br>
<br><i>Set it to start the task on a schedule corresponding to a few minutes before your driver refresh</i>
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
<br>        
<br><strong><i>Conditions</i></strong>
<br>
<br>Check wake the computer to run this task
<br>
<br><strong><i>Additional Notes</i></strong>        
<br>If you prefer to have a prebuilt example task to import, download the sample task and "Import Task" to Windows Task Scheduler. 
<br>You will need to change the "Start In" location so that the battery file is in the same location your Hubitat Driver reads.
