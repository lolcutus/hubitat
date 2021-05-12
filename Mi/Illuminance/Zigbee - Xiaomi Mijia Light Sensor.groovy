/**
 *  Copyright 2020 Lolcutus
 *
 *  Version v0.0.1.0004
 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "Zigbee Xiaomi Mijia Light Sensor", namespace: "lolcutus", author: "lolcutus") {
		capability "Battery"
		capability "Configuration"
		capability "PresenceSensor"
		capability "IlluminanceMeasurement"
		
		command "resetBatteryReplacedDate"
		command "checkMissed"
		command "resetLastUnknownMsg"
		
		attribute "batteryLastReplaced", "Date"
		attribute "lastUnknownMsg", "String"
		attribute "checksMissed", "Number"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000,0400,0003,0001", outClusters: "0003", manufacturer: "LUMI", model: "lumi.sen_ill.mgl01"

	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "showBatteryInfo", type: "bool", title: "Show battery messages in log", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
	}
}

private setVersion(){
	def map = [:]
 	map.name = "driver"
	map.value = "v0.0.1.0004"
	debugLog(map)
	updateDataValue(map.name,map.value)
 }
 def configure() {  
	setVersion()
	setDataForModels()
	if(device.currentValue("batteryLastReplaced") == null){
		 resetBatteryReplacedDate()
	}
}

	
// Parse incoming device messages to generate events
def parse(String description) {
	//init
	def MODEL = "0000_0005"
	def ILLUMINANCE = "0400_0000"
	def BATTERY01 = "0001_0020"
	// init end
	sendEvent(name: "presence", value: "present")
	sendEvent(name: "checksMissed", value: "0")
	debugLog("Parsing ${description}")
	def  msgMap = zigbee.parseDescriptionAsMap(description)
	debugLog(msgMap)
	if(!msgMap.containsKey("cluster")){
		return [:]
	}
	def cluster = msgMap["cluster"]
	def attrId = msgMap["attrId"]
	def encoding = Integer.parseInt(msgMap["encoding"],16)
	def valueHex = msgMap["value"]
	Map map = [:]
	
	def command = msgMap["cluster"] + '_' + msgMap["attrId"]
	debugLog("Command: ${command}")
	switch(command) {
		case MODEL:
			map.name = "model"
			map.value = valueHex
			infoLog(map)
			updateDataValue(map.name, map.value)
			setVersion()
			break
		case ILLUMINANCE:
			Integer rawValue = Integer.parseInt(valueHex, 16)
			debugLog("Value int: ${rawValue}")
			BigDecimal lux = rawValue > 0 ? Math.pow(10, rawValue / 10000.0) - 1.0 : 0
			lux = lux.setScale(0, BigDecimal.ROUND_HALF_UP)
			debugLog("Value lux: ${lux}")
			BigDecimal oldLux = device.currentValue('illuminance') == null ? null : device.currentValue('illuminance')
			debugLog("Old lux: ${oldLux}")
			map.name = "illuminance"
			map.value= lux
			map.unit = "lux"
			infoLog(map)
   			break
		case BATTERY01:
			map = parseBattery(valueHex)
			infoLog(map,showBatteryInfo)
			break
		default:
			map.name = "lastUnknownMsg"
			map.value = msgMap
			warnLog("Message not procesed: ${msgMap}")
	}
	return map
}

private setDataForModels(){
	def map = [:]
	def model = getDataValue("model");
 	state.comment = "Works with model GZCGQ01LM<BR>For presence to work you need to call 'checkMissed' with a rule one time each hour or more. Contact sensor send battery status each 50 minutes."
	debugLog("Model '${model}'")
	switch(model){
		case "lumi.sen_ill.mgl01":
			debugLog("Configure ${model}")
			if(getDataValue("manufacturer") == null){
				updateDataValue("manufacturer", "Lumi")
			}
			updateDataValue("modelName", "Xiaomi Mijia Light Sensor")
			updateDataValue("modelCode", "GZCGQ01LM")
			break
	}
	map	
}


private parseBattery(value) {
	def rawVolts = Integer.parseInt(value,16)/10
	debugLog("rawVolts: " + rawVolts)
	def minVolts = 2.7
	def maxVolts = 3.1
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
	def descText = "Battery level is ${roundedPct}% (${rawVolts} Volts)"
	
	def map = [:]
	map.name = "battery"
	map.value= roundedPct
	map.unit = "%"
	map.descriptionText = descText
	map
}

private resetBatteryReplacedDate() {
	sendEvent(name: "batteryLastReplaced", value: new Date())
}
private resetLastUnknownMsg() {
	sendEvent(name: "lastUnknownMsg", value: " ")
}



private checkMissed() {
	def currentMissed = device.currentValue("checksMissed")
	if(currentMissed == null){
		currentMissed = 2
	}
	currentMissed = currentMissed +1
	if(currentMissed > 10){
	currentMissed = 10
	}
	sendEvent(name: "checksMissed", value: currentMissed)
	if(currentMissed >= 2){
		sendEvent(name: "presence", value: "not present")
	}
}

def debugLog(msg){
	if(debugLogging == true){
		log.debug "["+device.getLabel() + "] " + msg
	}
}

def infoLog(msg,forced = false){
	if(infoLogging == true || forced){
		log.info "[" + device.getLabel() + "] " + msg
	}
}

def warnLog(msg){
	log.warn "[" + device.getLabel() + "] " + msg
}

def traceLog(msg){
	log.trace "[" + device.getLabel() + "] " + msg
}