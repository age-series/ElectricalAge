from PIL import Image
import numpy as np

source_mapping = {}
source_mapping["electric_components"] = "Electric Components 32x.png"
source_mapping["wires"] = "Cables 32x.png"
source_mapping["tools"] = "Tools 32x.png"

tile_size = 33

electric_components = [
    ["Transistor", "Power Transistor", "Round Transistor", "Round Power Transistor", "Six Chip", "Resonator"],
    ["Thermistor", "Photoresistor", "Electrolytic Capacitor", "Ceramic Capacitor", "Eight Chip",
     "Eight Chip with Sticker"],
    ["Diode", "Resistor", "Ten Chip", "Twenty Square Chip", "Gold Twenty Square Chip", "Button"],
    ["White LED", "Red LED", "Blue LED", "Green LED", "Yellow LED", "Variable Resistor"],
    ["Basic Motor", "Advanced Motor", "Inductor", "Toroidal Inductor", "Transformer", "3V Battery"],
    ["Piezo Element", "Speaker", "7 Segment Display", "Triple 7 Segment Display Chip", "Screen", "9V Battery"]]

wires = [["", "signalcable", "", "", "", ""],
         ["", "", "", "", "", ""],
         ["", "", "lowvoltagecable", "", "copperthermalcable", "mediumvoltagecable"],
         ["", "", "", "signalbuscable", "", ""],
         ["", "", "veryhighvoltagecable", "", "", "highvoltagecable"],
         ["", "", "", "", "", ""]]

tools = [["Magnet", "Signal Finder", "Thermometer", "Voltmeter", "Multimeter", ""],
         ["Wrench", "Screwdriver", "Antenna", "", "", ""],
         ["Basic Flashlight Off", "Basic Flashlight On", "Basic Flashlight Boosted", "Enhanced Flashlight Off",
          "Enhanced Flashlight On", "Enhanced Flashlight Boosted"],
         ["Iron Clutch Plate", "Copper Clutch Plate", "Tungsten Clutch Plate", "Gold Clutch Plate", "Coal Clutch Plate",
          "Clutch Pin"], ["", "", "", "", "", "Steel Clutch Plate"],
         ["", "", "", "", "", ""]]

for sprite_set in source_mapping:
    source_image = Image.open(source_mapping[sprite_set])
    width, height = source_image.size

    sprite_names = []

    if sprite_set == "electric_components":
        sprite_names = electric_components
    if sprite_set == "wires":
        sprite_names = wires
    if sprite_set == "tools":
        sprite_names = tools

    if sprite_set == "wires":
        #  Do the red replacement with transparency
        raw_src_img = source_image.convert('RGBA')

        raw_src_data = np.array(raw_src_img)   # "data" is a height x width x 4 numpy array
        red, green, blue, alpha = raw_src_data.T # Temporarily unpack the bands for readability

        # Replace white with red... (leaves alpha values alone...)
        red_areas = (red == 214) & (blue == 0) & (green == 0)
        raw_src_data[red_areas.T] = (0, 0, 0, 0)  # Transpose back needed

        new_image = Image.fromarray(raw_src_data)
        source_image = new_image

    # Save Chops of original image
    for x0 in range(0, width, tile_size):
        for y0 in range(0, height, tile_size):
            y_index = int(y0/32)
            x_index = int(x0/32)
            try:

                vertical_shift = 3

                export_name = sprite_names[y_index][x_index]
                if export_name != "":
                    box = (x0 + 1, y0 + 1,
                           (x0 + tile_size if x0 + tile_size < width else width - 1) - 1,
                           (y0 + tile_size if y0 + tile_size < height else height - 1) - 1)

                    new_image = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
                    new_image.paste(source_image.crop(box), (1, 1 + vertical_shift))

                    if sprite_set == "wires":
                        # We need to remove the cable color palette
                        overlay = Image.new('RGBA', (4, 4), (0, 0, 0, 0))
                        new_image.paste(overlay, (1, 1 + vertical_shift))

                    new_image.save("{}/{}.png".format(sprite_set, export_name, x0, y0))
            except:
                print("Tried to index {} {}, but didn't go well.".format(y_index, x_index))
