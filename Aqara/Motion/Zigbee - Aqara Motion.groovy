/**
 *  Copyright 2020 Lolcutus
 *
 *  Version v1.0.7.0000
 
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

#include lolcutus.driverUtilities
#include lolcutus.aqaraUtilities

metadata {
	definition (name: "Zigbee - Aqara Motion", namespace: "lolcutus", author: "lolcutus", importUrl: "https://raw.githubusercontent.com/lolcutus/hubitat/master/Aqara/Motion/Zigbee%20-%20Aqara%20Motion.groovy") {
		capability "Battery"
		capability "Configuration"
		capability "PresenceSensor"
		capability "IlluminanceMeasurement"
		capability "MotionSensor"
		
		command "resetBatteryReplacedDate"
		command "checkMissed"
		command "setToActive"
		command "setToInactive"
		command "updateDriverInfo"
		
		attribute "batteryLastReplaced", "Date"
		attribute "lastUnknownMsg", "String"
		attribute "checksMissed", "Number"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "00C6", inClusters: "0000,FFFF,0406,0400,0500,0001,0003", outClusters: "0000,0019", manufacturer: "LUMI", model: "lumi.sensor_motion.aq2", aplication:"05"
	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "showBatteryInfo", type: "bool", title: "Show battery messages in log", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "resetTimeSetting", type: "number", title:"Reset Motion Timer", description: "After X number of seconds, reset motion to inactive (1 to 3600, default: 15)", defaultValue: "15", range: "1..3600")
	}
}

 def configure() {
	setDataForModels()
 	setVersion("v1.0.7.0000")
 	if(device.currentValue("batteryLastReplaced") == null){
		 resetBatteryReplacedDate()
	}
 }

	
// Parse incoming device messages to generate events
def parse(String description) {
	//init
	def MODEL = "0000_0005"
	def ILLUMINATION = "0400_0000"
	def MOTION = "0406_0000"
	def BATTERY01 = "0000_FF01"
	
	messageReceived()
	
	debugLog("Parsing ${description}")
	Map msgMap = parseDescription(description)
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
	debugLog("Command: ${command} - value: ${valueHex}")
	switch(command) {
		case MODEL:
			map.name = "model"
			map.value = valueHex
			updateDataValue(map.name, map.value)
			setVersion()
			infoLog(map)
			break
		case ILLUMINATION:
			Integer lux = Integer.parseInt(msgMap['value'], 16)
			map.name ="illuminance" 
			map.value = lux
			map.unit = "lux"
			map.isStateChange = true
			infoLog(map)
			break
		case MOTION:
			setToActive()
			infoLog(map)
			break
		case BATTERY01:
			map = parseBattery(msgMap["value"])
			infoLog(map,showBatteryInfo)
			break
		default:
			map.name = "lastUnknownMsg"
			map.value = msgMap
			warnLog("Message not procesed: ${msgMap}")
	}
	return map
}


void setToActive() {
	def map = [:]
	debugLog("set active()")
	map.name ="motion" 
	map.value = "active"
	map.isStateChange = true
	Integer resetTime = resetTimeSetting != null ? resetTimeSetting : 15
	runIn(resetTime, "setToInactive")
	sendEvent(map)
	infoLog(map)
}

void setToInactive() {
	debugLog("setToInactive()")
	def map = [:]
	map.name ="motion" 
	map.value = "inactive"
	map.isStateChange = true
	sendEvent(map)
	infoLog(map)
}

private setDataForModels(){
	def map = [:]
	def model = getDataValue("model");
	if(model.length() > "lumi.sensor_motion.aq2".length() && model.startsWith("lumi.sensor_motion.aq2")){
		model =  "lumi.sensor_motion.aq2"
		updateDataValue("model", model)
	}
	state.comment = "Works with model RTCGQ11LM<BR>For presence to work you need to call 'checkMissed' with a rule one time each hour or more. Contact sensor send battery status each 50 minutes."
	debugLog("Model '${model}'")
	switch(model){
		case "lumi.sensor_motion.aq2":
			debugLog("Configure ${model}")
			if(getDataValue("manufacturer") == null){
				updateDataValue("manufacturer", "Lumi")
			}
			updateDataValue("modelName", "Aqara Body Sensor")
			updateDataValue("modelCode", "RTCGQ11LM")
			break
		
	}
}
