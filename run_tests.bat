@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"C:\Users\PC\Documents\Dev\Brohouse_android\gradlew.bat" -p "C:\Users\PC\Documents\Dev\Brohouse_android" :app:testDebugUnitTest --tests "com.bennybokki.frientrip.CostCalculatorTest" --tests "com.bennybokki.frientrip.InviteLogicTest" --tests "com.bennybokki.frientrip.TripCreationTest" --stacktrace 2>&1
