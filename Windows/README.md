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
<br>If you prefer to have a prebuilt sample task to import, <a href="https://github.com/pentalingual/Hubitat/blob/main/Windows/Task%20Post%20battery%20Script.xml">download the sample task</a> and use "Import Task" in Windows Task Scheduler. 
<br><strong>IMPORTANT:You will need edit your desired "Start In" location if you use the prebuilt sample task</strong> 
<br>You can change this after importing by clicking into edit EACH action and changing the Start In location.
<br>There are two actions, both need to be changed to start in the same location.
<br>The battery file needs to start in the same location that your Hubitat Driver reads.
<br>The battery file will be called '"batteryreport.txt"'
