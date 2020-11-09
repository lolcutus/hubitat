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
		
		attribute "version", "String"
		attribute "batteryLastReplaced", "String"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000,0003,FFFF,0019", outClusters: "0000,0004,0003,0006,0008,0005,0019", manufacturer: "LUMI", model: "lumi.sensor_magnet"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_magnet.aq2"
	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
	}
}

private setVersion(){
	def map = [:]
 	map.name = "version"
	map.value = "v0.0.1.0001"
	debugLog(map)
	sendEvent(map)
 }
 def configure() {  
 	setVersion()
 }

	
// Parse incoming device messages to generate events
def parse(String description) {
	debugLog("Parsing ${description}")
	def  msgMap = zigbee.parseDescriptionAsMap(description)
	debugLog(msgMap)
	if(!msgMap.containsKey("cluster")){
		return [:]
	}
	def cluster = msgMap["cluster"]
	//debugLog("cluster: "+cluster)
	def attrId = msgMap["attrId"]
	//debugLog("attrId: "+attrId)
	def encoding = Integer.parseInt(msgMap["encoding"],16)
	//debugLog("encoding: "+encoding)
	def valueHex = msgMap["value"]
	//debugLog("valueHex: "+valueHex)
	Map map = [:]
	if (cluster == "0000" && attrId == "0005"){
		map.model = valueHex
		updateDataValue('model', valueHex)           
	}
	infoLog(map)
  	return map
}

def debugLog(msg){
	if(debugLogging == true){
		   log.debug "["+device.getLabel() + "] " + msg
	}
}

def infoLog(msg){
	if(infoLogging == true){
		   log.info "[" + device.getLabel() + "] " + msg
	}
}
def warnLog(msg){
		   log.warn "[" + device.getLabel() + "] " + msg
}

def traceLog(msg){
		   log.trace "[" + device.getLabel() + "] " + msg
}