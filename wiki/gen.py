
list = """
200V Compressor
200V Heat Turbine
200V Macerator
200V Thermal Dissipator
200V example build
50V Compressor
50V Egg Incubator
50V Fuel Generator
50V Heat Turbine
50V Macerator
50V Magnetizer
50V Plate Machine
50V Thermal Dissipator
50V example build
AND (Chip)
Actuator
Advanced Magnet
Alarm
Alloy Dust
Alloy Ingot
Amplifier (Chip)
Analogic Regulator
Auto Miner
Autominer
Autominer example
Basic Magnet
Battery
Battery Charger
Breaker
Cable
Cables
Capacitor
Capacitors
Captor
Cheap Chip
Chips
Circuits
Coal
Coal Dust
Cobblestone
Codebase
Combustion Chamber
Compressor
Condensator
Config Copy Tool
Configurable summing unit (Chip)
Copper Armor
Copper Cable
Copper Ingot
Copper Thermal Cable
DC-DC Convertor
D Flip-Flop (Chip)
Data Logger
Daylight Sensor
Developing
Diamond
Dielectric
Digital Display
Digital Watch
Ecoal
Electrical Age to other energy exporter
Electrical Breaker
Electrical Cables
Electrical Fire Buzzer
Electrical Motor
Electrical Source
Electricity
Eln Computer Probe
Examples
FAQ
Ferromagnetic Core
Flashlight
Flat Lamp Socket
Flint
Fluorescent Lamp Socket
Flywheel
Furnace
Gas turbine
Generator
Glass Pane
Gold Ingot
Gold Plate
Ground Cable
Heat Turbine
High Voltage Cable
High Voltage Relay
Hub
Inductor
Iron Door
Iron Ingot
Iron Plate
JK Flip-Flop (Chip)
Lamp
Lamp Socket A
Lamp Socket B Projector
Lamp Supply
Large Rheostat
Light Bulb
Light Sensor
List of chips
Long Suspended Lamp Socket
Low Voltage Cable
Low Voltage Relay
Macerator
Machine
Machine Block
Machine Booster
Magnetizer
Main Page
Matrix size
Medium Voltage Cable
Medium Voltage Relay
Miner
Modbus RTU
Modified Nodal Analysis
NAND (Chip)
NOR (Chip)
NOT (Chip)
Nixie Tube
Note Block
OR (Chip)
OpAmp (Chip)
Oscillator (Chip)
PAL (Chip)
PID Regulator (Chip)
Parallelized turbine example
Plate Machine
Portable Battery
Portable Capacitor
Portable Condensator
Portable mining drill
Powerline example
Powersystem example build
Redstone
Redstone Torch
Relay
Remote Receiver
Remote Transmitter
Replicator
Resin
Resistor
Rheostat
Robust Lamp Socket
Rubber
Sandbox
Schmitt Trigger (Chip)
Sensor
Signal Button
Signal Cable
Signal Processor
Signal Receiver
Signal Repeater
Signal Source
Signal Transmitter
Silicon Dust
Silicon Ingot
Simple Lamp Socket
Simple gas turbine control
Solar Panel
Solar Tracker
Solar panel
Steam turbine
Steam turbine example
Stone
Stone Heat Furnace
Street Lamp
Suspended Lamp Socket
Switch
Tachometer
Test Page
Thermal Dissipator
Tier 2 Powerline Example
Transformator
Transformer
Transmitter Antenna
Transportation Machine
Transporter
Tree Resin
Tree Resin Collector
Tuto electrical
Unreleasedium
Upcoming Powerline example
Utility pole
Very High Voltage Cable
Very High Voltage Relay
Voltage controlled amplifier (Chip)
Voltage controlled sine oscillator (Chip)
Wind turbine
Wrench
X-Ray Scanner
XNOR (Chip)
XOR (Chip)
XRay scanner
"""

import os
from bs4 import BeautifulSoup

import urllib.parse
import urllib.request

pages = [x.strip() for x in list.split("\n") if x.strip() != ""]

def create_files():
    for page in pages:
        filename = f"md/{page}.md".replace(" ", "_")
        if filename not in os.listdir("md"):
            print(f"Missing {filename}")
            f = open(filename, "w+")
            f.close()

def store_content(page_name, content):
    filename = f"html/{page_name}.html".replace(" ", "_")
    if filename not in os.listdir("html"):
        f = open(filename, "w+")
        f.write(content)
        f.close()

def fetch_content(page_name):
    failed_pages = []

    prefix = "https://wiki.electrical-age.net/index.php?title="
    suffix = "&action=edit"
    clean_page_name = urllib.parse.quote(page_name)
    page_uri = f"{prefix}{clean_page_name}{suffix}"

    with urllib.request.urlopen(page_uri) as response:
        html = response.read().decode("UTF-8").strip()

        if html == "Exception encountered, of type &quot;Error&quot;":
            print(f"Could not fetch {page_name}")
            failed_pages.append(page_name)
        else:
            print(f"Storing {page_name}")
            store_content(page_name, html)

    f = open("failed_pages.txt", "w+")
    f.write(", ".join(failed_pages))
    f.close()


def fetch_all_content():
    for page in pages:
        fetch_content(page)

# create_files()
# fetch_all_content()

def get_page_content(content):
    # Create a BeautifulSoup object and specify the parser
    soup = BeautifulSoup(content, 'html.parser')

    # Find the element with id "wpTextbox1"
    textbox = soup.find(id='wpTextbox1')

    if textbox is not None:
        return textbox.get_text()  # Print the text within the textbox
    else:
        print("Element with id 'wpTextbox1' not found")

def get_all_textboxes():
    for page in pages:
        html_filename = f"html/{page}.html".replace(" ", "_")
        text_filename = f"text/{page}.txt".replace(" ", "_")
        try:
            with open(html_filename) as html_file:
                with open(text_filename, "w+") as text_file:
                    content = get_page_content(html_file.read())
                    if content:
                        text_file.write(content)
        except:
            pass

# get_all_textboxes()

from pprint import pprint

def check_for_links(content):
    image_sections = content.split("[[Image:")[1:]
    images = [sec.split("|", 1)[0] for sec in image_sections]

    file_sections = content.split("[[File:")[1:]
    files = [sec2.split("|", 1)[0] for sec2 in [sec.split("]]", 1)[0] for sec in file_sections]]

    return images, files



def get_all_server_files():
    files_list = []
    images_list = []

    for page in pages:
        text_filename = f"text/{page}.txt".replace(" ", "_")
        try:
            with open(text_filename) as text_file:

                content = text_file.read()
                if "REDIRECT" in content:
                    continue

                images, files = check_for_links(content)

                files_list.extend(files)
                images_list.extend(images)
        except:
            print(f"Could not read {page}")

    return set(files_list).union(set(images_list))


pprint(get_all_server_files())