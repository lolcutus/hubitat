library (
	base: "driver",
	author: "lolcutus",
	category: "driverUtilities",
	description: "driverUtilities",
	name: "driverUtilities",
	namespace: "lolcutus",
	documentationLink: ""
)
	
void checkMissed() {
	def currentMissed = device.currentValue("checksMissed")
	if(currentMissed == null){
		currentMissed = 2
	}
	currentMissed = currentMissed +1
	if(currentMissed > 10){
	currentMissed = 10
	}
	sendEvent(name: "checksMissed", value: currentMissed)
	if(currentMissed == 2){
		sendEvent(name: "presence", value: "not present")
	}
}
void messageReceived(){
	def presence = device.currentValue("presence")
	if(!presence){
		sendEvent(name: "presence", value: "present")
	}
	def currentMissed = device.currentValue("checksMissed")
	if(currentMissed == null || currentMissed!=0){
		sendEvent(name: "checksMissed", value: "0")
	}
}

void resetBatteryReplacedDate() {
	sendEvent(name: "batteryLastReplaced", value: new Date())
}

void debugLog(msg){
	if(debugLogging == true){
		log.debug "["+device.getLabel() + "] " + msg
	}
}

void infoLog(msg,forced = false){
	if(infoLogging == true || forced){
		log.info "[" + device.getLabel() + "] " + msg
	}
}
void warnLog(msg){
	log.warn "[" + device.getLabel() + "] " + msg
}

void traceLog(msg){
	log.trace "[" + device.getLabel() + "] " + msg
}

void setVersion(version){
	def map = [:]
	map.name = "driver"
	map.value = version
	debugLog(map)
	updateDataValue(map.name,map.value)
}
 