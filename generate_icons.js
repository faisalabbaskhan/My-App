const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('Installing jimp for icon processing...');
execSync('npm install jimp@0.16.1', { stdio: 'inherit' });

const Jimp = require('jimp');

const srcPath = path.join(__dirname, 'app', 'src', 'main', 'res', 'drawable', 'gymora_logo.jpg');
const outputBase = path.join(__dirname, 'app', 'src', 'main', 'res');

const densities = {
  'mdpi': 48,
  'hdpi': 72,
  'xhdpi': 96,
  'xxhdpi': 144,
  'xxxhdpi': 192
};

async function generate() {
  if (!fs.existsSync(srcPath)) {
    console.error(`Source image not found at ${srcPath}`);
    process.exit(1);
  }

  const image = await Jimp.read(srcPath);

  for (const [density, size] of Object.entries(densities)) {
    const dir = path.join(outputBase, `mipmap-${density}`);
    if (!fs.existsSync(dir)){
      fs.mkdirSync(dir, { recursive: true });
    }

    // Square icon
    const square = image.clone().clone().resize(size, size);
    await square.writeAsync(path.join(dir, 'ic_launcher.png'));

    // Round / circular icon
    const round = image.clone().clone().resize(size, size).circle();
    await round.writeAsync(path.join(dir, 'ic_launcher_round.png'));

    console.log(`Successfully generated icons for mipmap-${density} at size ${size}x${size}`);
  }
}

generate().catch(err => {
  console.error(err);
  process.exit(1);
});
