/**
 *  Copyright 2020 Lolcutus
 *
 *  Version v1.0.2.0003
 
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
	definition (name: "Zigbee - Aqara Contact", namespace: "lolcutus", author: "lolcutus", importUrl: "https://raw.githubusercontent.com/lolcutus/hubitat/master/Aqara/Contact/Zigbee%20-%20Aquara%20Contact.groovy") {
		capability "Battery"
		capability "Configuration"
		capability "Contact Sensor"
		
		command "resetBatteryReplacedDate"
		
		attribute "batteryLastReplaced", "Date"
		attribute "lastUnknownMsg", "String"
	
		fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "Lumi", model: "lumi.sensor_magnet.aq2"
	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
	}
}

private setVersion(){
	def map = [:]
 	map.name = "driver"
	map.value = "v1.0.2.0003"
	debugLog(map)
	updateDataValue(map.name,map.value)
	state.remove("prefsSetCount")
    removeDataValue("application")
 }
def configure() {  
	setVersion()
	state.comment = "Works with model MCCGQ11LM"
	if(batteryLastReplaced == null){
		 resetBatteryReplacedDate()
	}
 }

	
// Parse incoming device messages to generate events
def parse(String description) {
    //init
    def MODEL = "0000_0005"
    def CONTACT = "0006_0000"
    def BATTERY01 = "0000_FF01"
    //end Init
	//debugLog("Parsing ${description}")
    def  msgMap = zigbee.parseDescriptionAsMap(description)
	debugLog(msgMap)
	if(!msgMap.containsKey("cluster")){
		return [:]
	}
	def valueHex = msgMap["value"]
	Map map = [:]
	def command = msgMap["cluster"] + '_' + msgMap["attrId"]
	debugLog("Command: ${command}")
	switch(command) {
		case MODEL:
			map.name = "model"
			map.value = valueHex
			updateDataValue(map.name, map.value)
			if(map.value == "lumi.sensor_magnet.aq2" && getDataValue("manufacturer") == null){
				updateDataValue("manufacturer", "Lumi")
			}
            setVersion()
			break
		case CONTACT:
			map = parseContact(Integer.parseInt(valueHex))
			break
		case BATTERY01:
			if(msgMap["encoding"] == "42") {
				msgMap = zigbee.parseDescriptionAsMap(description.replace('encoding: 42', 'encoding: 41'))
			}
			map = parseBattery(msgMap["value"])
			break
		default:
			map.name = "lastUnknownMsg"
			map.value = msgMap
			warnLog("Message not procesed: ${msgMap}")
	}
	infoLog(map)
   	return map
}
                
private parseContact(closedOpen) {
    debugLog("Value ${closedOpen}")
    def map = [:]
	map.name = "contact"
    if(closedOpen == 0){
        map.value = "closed"
    }else{
        map.value = "open"
    }
	map.descriptionText = "Contact was ${map.value}!"
	map.isStateChange = true
    map
}

private parseBattery(value) {
    def batteryVoltajeFirstIndex = 6 
    def batteryVoltajeSecondIndex = 5
    
    def batteryVoltaje = value[batteryVoltajeFirstIndex .. (batteryVoltajeFirstIndex+1)] + value[batteryVoltajeSecondIndex .. (batteryVoltajeSecondIndex+1)]
    def rawVolts = Integer.parseInt(batteryVoltaje,16)/1000
    
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