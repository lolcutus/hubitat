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
	definition (name: "Zigbee - Aqara Buttons - no Release", namespace: "lolcutus", author: "lolcutus", importUrl: "https://raw.githubusercontent.com/lolcutus/hubitat/master/Aqara/Buttons/Zigbee%20-%20Aqara%20Buttons%20no%20Release.groovy") {
		capability "Battery"
		capability "Configuration"
		capability "DoubleTapableButton"
		capability "HoldableButton"
		capability "PushableButton"
		capability "PresenceSensor"
		
		command "resetBatteryReplacedDate"
		command "checkMissed"
		command "updateDriverInfo"
		
		attribute "batteryLastReplaced", "Date"
		attribute "lastUnknownMsg", "String"
		attribute "checksMissed", "Number"

		fingerprint endpointId : "01", profileId: "0104", deviceId: "01A1", inClusters: "0000,0003,0019,FFFF,0012", outClusters: "0000,0004,0003,0005,0019,FFFF,0012", manufacturer: "LUMI", model: "lumi.remote.b186acn01", aplication:"09"
		fingerprint endpointId : "01", profileId: "0104", deviceId: "01A1", inClusters: "0000,0003,0019,FFFF,0012", outClusters: "0000,0004,0003,0005,0019,FFFF,0012", manufacturer: "LUMI", model: "lumi.remote.b286acn01", aplication:"09"
	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "showBatteryInfo", type: "bool", title: "Show battery messages in log", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
	}
}

def configure() {  
	def map = setDataForModels()
	sendEvent(map)
 	setVersion("v1.0.7.0000")
 	if(device.currentValue("batteryLastReplaced") == null){
		 resetBatteryReplacedDate()
	}
	state.remove("prefsSetCount")
	state.remove("forcedMinutes")
	state.remove("numOfButtons")
}

	
// Parse incoming device messages to generate events
def parse(String description) {
  	//init
	def MODEL = "0000_0005"
	def BATTERY01 = "0000_FF01"
	def BUTTON01 = "0012_0055"
	def NOTKNOWN = "0000_FFF0"
	
	messageReceived()
	
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
	debugLog("Command: ${command}")
	switch(command) {
		case MODEL:
		  	map = setDataForModels()
			setVersion()
			infoLog(map)
			break
		case BATTERY01:
			map = parseBattery(msgMap["value"])
			infoLog(map,showBatteryInfo)
			break
		case BUTTON01:
			map = parseButtonMessage(msgMap)
			infoLog(map)
			break
		case NOTKNOWN:
			warnLog("Not knowing what to do with ${msgMap}")
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

private parseButtonMessage(msgMap) {
	def buttonNum =  Integer.parseInt(msgMap.endpoint)
	debugLog("Button ${buttonNum}")
	def existingButtons = getDataValue("physicalButtons")
	if(buttonNum > existingButtons ){
		warnLog("Unexpected button. this model has ${existingButtons} buttons but pressed was ${buttonNum}.")
		return []
	}
	def eventType = Integer.parseInt(msgMap.value[2..3],16)
	debugLog("EventType ${eventType}")
	
	def map = [:]
	switch (eventType){
		case 1:
			map.name ="pushed" 
			break
		case 0:
			map.name ="held" 
			break
		case 2:
			map.name ="doubleTapped" 
			break
	}
	map.value = buttonNum
	map.isStateChange = true
	map
}


