/**
 *  Copyright 2020 Lolcutus
 *
 *  Version v0.0.1.0001
 
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
	definition (name: "Zigbee Driver Template", namespace: "lolcutus", author: "lolcutus") {
		capability "Battery"
		capability "Configuration"
		capability "PresenceSensor"
		
		command "resetBatteryReplacedDate"
		command "checkMissed"
		command "resetLastUnknownMsg"
		
		attribute "batteryLastReplaced", "Date"
		attribute "lastUnknownMsg", "String"
		attribute "checksMissed", "Number"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000,0003,FFFF,0019", outClusters: "0000,0004,0003,0006,0008,0005,0019", manufacturer: "LUMI", model: "lumi.sensor_magnet"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_magnet.aq2"
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
	map.value = "v0.0.1.0001"
	debugLog(map)
	updateDataValue(map.name,map.value)
 }
 def configure() {  
 	setVersion()
 	setDataForModels()
 	if(device.currentValue("batteryLastReplaced") == null){
		 resetBatteryReplacedDate()
	}
	//change device details
	//device.updateDataValue("test","testaa")
	//device.removeDataValue("lastUnknownMsg")
	//unschedule()
 }

	
// Parse incoming device messages to generate events
def parse(String description) {
	//init
	def MODEL = "0000_0005"
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
		default:
			map.name = "lastUnknownMsg"
			map.value = msgMap
			warnLog("Message not procesed: ${msgMap}")
	}
	
   	return map
}

private resetBatteryReplacedDate() {
	sendEvent(name: "batteryLastReplaced", value: new Date())
}

private setDataForModels(){
	def map = [:]
	def model = getDataValue("model");
   if(model.length() > "lumi.remote.b186acn01".length() && model.startsWith("lumi.remote.b186acn01")){
		model =  "lumi.remote.b186acn01"
		updateDataValue("model", model)
	}
	if(model.length() > "lumi.remote.b286acn01".length() && model.startsWith("lumi.remote.b286acn01")){
		model =  "lumi.remote.b286acn01"
		updateDataValue("model", model)
	}
	state.comment = "Works with model WXKG03LM, WXKG02LM<BR>For presence to work you need to call 'checkMissed' with a rule one time each hour or more. Contact sensor send battery status each 50 minutes."
	debugLog("Model '${model}'")
	switch(model){
		case "lumi.remote.b186acn01":
			debugLog("Configure ${model}")
			if(getDataValue("manufacturer") == null){
				updateDataValue("manufacturer", "Lumi")
			}
			updateDataValue("modelName", "Aqara 1-button Light Switch (WXKG03LM - 2018)")
			updateDataValue("modelCode", "WXKG03LM")
			updateDataValue("physicalButtons", "1")
			map.name = "numberOfButtons"
			map.value = 1
			break
		case "lumi.remote.b286acn01":
			debugLog("Configure ${model}")
			if(getDataValue("manufacturer") == null){
				updateDataValue("manufacturer", "Lumi")
			}
			updateDataValue("modelName", "Aqara 2-button Light Switch (WXKG02LM - 2018)")
			updateDataValue("modelCode", "WXKG02LM")
			updateDataValue("physicalButtons", "3")
			map.name = "numberOfButtons"
			map.value = 3
			break
	}
	map
	
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

private resetLastUnknownMsg() {
	sendEvent(name: "lastUnknownMsg", value: " ")
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