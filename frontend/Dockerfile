FROM node:20-slim

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm install

# Copy the rest of the code
COPY . .

EXPOSE 5173

# Run the Vite development server
CMD ["npm", "run", "dev", "--", "--host"]