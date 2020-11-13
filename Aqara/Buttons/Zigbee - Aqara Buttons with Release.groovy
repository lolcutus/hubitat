/**
 *  Copyright 2020 Lolcutus
 *
 *  Version v1.0.1.0000
 
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
	definition (name: "Zigbee - Aqara Buttons - with release", namespace: "lolcutus", author: "lolcutus", importUrl: "https://raw.githubusercontent.com/lolcutus/hubitat/master/Aqara/Buttons/Zigbee%20-%20Aqara%20Buttons%20with%20Release.groovy") {
		capability "Battery"
		capability "Configuration"
		capability "DoubleTapableButton"
		capability "HoldableButton"
		capability "PushableButton"
		capability "ReleasableButton"
		
		command "resetBatteryReplacedDate"
		
		attribute "batteryLastReplaced", "Date"
		attribute "lastUnknownMsg", "String"

		fingerprint endpointId : "01", profileId: "0104", deviceId: "059D", inClusters: "0000,0012,0003", outClusters: "0000", manufacturer: "LUMI", model: "lumi.remote.b1acn01", aplication:"02"
	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
	}
}

private setVersion(){
	def map = [:]
 	map.name = "driver"
	map.value = "v1.0.1.0000"
	debugLog(map)
	updateDataValue(map.name,map.value)
 }
 def configure() {  
	def map = setDataForModels()
	sendEvent(map)
 	setVersion()
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
	// init end
	debugLog("Parsing ${description}")
	def  msgMap
	if(description.indexOf('attrId: FF01, encoding: 42') >= 0) {
		msgMap = zigbee.parseDescriptionAsMap(description.replace('encoding: 42', 'encoding: F2'))
		msgMap["encoding"] = "41"
	}else{
		msgMap = zigbee.parseDescriptionAsMap(description)
	}
	
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
			break
		case BATTERY01:
			map = parseBattery(msgMap["value"])
			break
		case BUTTON01:
			map = parseButtonMessage(msgMap)
			break
		case NOTKNOWN:
			warnLog("Not knowing what to do with ${msgMap}")
			break
		default:
			map.name = "lastUnknownMsg"
			map.value = msgMap
			warnLog("Message not procesed: ${msgMap}")
	}
	infoLog(map)
   	return map
}

private setDataForModels(){
	def map = [:]
	def model = getDataValue("model");
	if(model.length() > "lumi.remote.b1acn01".length() && model.startsWith("lumi.remote.b1acn01")){
		model =  "lumi.remote.b1acn01"
		updateDataValue("model", model)
	}
  state.comment = "Works with model WXKG11LM"
	debugLog("Model '${model}'")
	switch(model){
		case "lumi.remote.b1acn01":
			debugLog("Configure ${model}")
			if(getDataValue("manufacturer") == null){
				updateDataValue("manufacturer", "Lumi")
			}
			updateDataValue("modelName", "Aqara Wireless Mini Switch")
			updateDataValue("modelCode", "WXKG11LM")
			updateDataValue("physicalButtons", "1")
			map.name = "numberOfButtons"
			map.value = 1
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
		case 255:
			map.name ="released" 
			break
		
	}
	map.value = buttonNum
	map.isStateChange = true
	map
}

private parseBattery(value) {
	def batteryVoltajeFirstIndex
	def batteryVoltajeSecondIndex
	def model = getDataValue("model");
	switch(model){
		case "lumi.remote.b1acn01":
			batteryVoltajeFirstIndex = 8 
			batteryVoltajeSecondIndex = 6
			break
	}
	def batteryVoltaje = value[batteryVoltajeFirstIndex .. (batteryVoltajeFirstIndex+1)] + value[batteryVoltajeSecondIndex .. (batteryVoltajeSecondIndex+1)]
	debugLog("batteryVoltaje: " + batteryVoltaje)
	def rawVolts = Integer.parseInt(batteryVoltaje,16)/1000
	debugLog("rawVolts: " + rawVolts)
	def minVolts = 2.5
	def maxVolts = 3.0
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
	def descText = "Battery level is ${roundedPct}% (${rawVolts} Volts)"
	
	def map = [:]
	map.name = "battery"
	map.value= roundedPct
	map.unit = "%"
	map.descriptionText = descText
	infoLog(map,true)
	map
	
	
}

private resetBatteryReplacedDate() {
	sendEvent(name: "batteryLastReplaced", value: new Date())
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