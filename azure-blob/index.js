const express = require('express');
const multer = require('multer');
const { BlobServiceClient, StorageError } = require('@azure/storage-blob');
const path = require('path');
var admin = require("firebase-admin");
const { MIMEType } = require('util');
const dotEnv = require('dotenv');
dotEnv.config();
var serviceAccount = require(process.env.FIREBASEACCOUNTKEY);
admin.initializeApp({

    credential: admin.credential.cert(serviceAccount),
  
    databaseURL: "https://menu-mate-d4119-default-rtdb.asia-southeast1.firebasedatabase.app"
  
  })

const app = express();
const port = 3000;
const firestore= admin.firestore();


const AZURITE_ACCOUNT_NAME = 'devstoreaccount1';
const AZURITE_ACCOUNT_KEY = 'Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==';
const AZURITE_BLOB_URL = 'http://127.0.0.1:10000/devstoreaccount1';

// Azurite connection string (local development)
const connectionString = "UseDevelopmentStorage=true";
const blobServiceClient = BlobServiceClient.fromConnectionString(
  `DefaultEndpointsProtocol=http;AccountName=${AZURITE_ACCOUNT_NAME};AccountKey=${AZURITE_ACCOUNT_KEY};BlobEndpoint=${AZURITE_BLOB_URL};`
);
const containerName = "userfiles";

// Create container if it doesn't exist
async function initializeContainer() {
    try {
        const containerClient = blobServiceClient.getContainerClient(containerName);
        await containerClient.createIfNotExists();
        console.log("Container initialized");
    } catch (error) {
        console.error("Error initializing container:", error);
    }
}

initializeContainer();

// Configure multer for file upload
const upload = multer({
    storage: multer.memoryStorage(),
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB limit
    }
});

// Upload file
app.post('/upload', upload.single('file'), async (req, res) => {
    console.log("req received")
    try {
        const {filePath} = req.query
        const file = req.file;

        if (!file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        const blobName = `${filePath}/${file.originalname}`;
        const containerClient = blobServiceClient.getContainerClient(containerName);
        const blockBlobClient = containerClient.getBlockBlobClient(blobName);

        // Upload with overwrite
        const options = {
            blobHTTPHeaders: {
                blobContentType: file.mimetype
            }
        };

        await blockBlobClient.upload(file.buffer, file.size, options);

        res.status(200).json({
            message: 'File uploaded successfully',
            path: blobName,
            size: file.size,
            mimetype: file.mimetype
        });
    } catch (error) {
        console.error('Upload error:', error);
        res.status(400).json({ error: 'Upload failed' });
    }
});

app.get('/download', async (req, res) => {
    try {
        const { uid, image } = req.query;
        const blobName = `restaurants/${uid}/menuItems/${image}.jpg`;
        console.log("path: "+blobName)
        const containerClient = blobServiceClient.getContainerClient(containerName);
        const blockBlobClient = containerClient.getBlockBlobClient(blobName);

        const downloadBlockBlobResponse = await blockBlobClient.download();
        const downloadedData = await streamToBuffer(downloadBlockBlobResponse.readableStreamBody);
        
        res.status(200).send(downloadedData);
    } catch (error) {
        console.error(error)
        res.status(500).json({ error: error.message });
    }
});

async function streamToBuffer(readableStream) {
    return new Promise((resolve, reject) => {
        const chunks = [];
        readableStream.on("data", (data) => {
            chunks.push(data instanceof Buffer ? data : Buffer.from(data));
        });
        readableStream.on("end", () => {
            resolve(Buffer.concat(chunks));
        });
        readableStream.on("error", reject);
    });
}
app.post('/registerReview/:postID',async (req,res)=>{
    const { uid ,stars} = req.query;
    const postId= req.params['postID'];
    const itemRef = firestore.collection('restaurants').doc(uid).collection('menu_items').doc(postId);
    itemRef.collection('reviews').doc(uid).get().then((doc)=>{
        if(doc.exists){
            res.status(400).json({message:"User has already review this item"})
        }else{
            itemRef.get().then((itemInstance)=>{
                var rating =itemInstance.data().rating;
                var starsSum = itemInstance.data().stars;
                var noOfusers = 0;
                if(rating!=0){
                  const noOfusers = stars / rating ;
                }
                starsSum+=stars;
                noOfusers++;
                rating = (starsSum/noOfusers).toFixed(1);
                const batch = firestore.batch();
                batch.set(itemInstance.ref,{stars:starsSum,rating:rating},{merge :true});
                batch.set(doc.ref,{stars:stars});
                batch.commit();
            res.status(200)
            });
           /* 
            
            batch.update()*/
        }
    }).catch((error)=>{
        console.error(error);
        //res.status(400).json({message:error})
    });
    //itemRef.set({})
    res.status(200).send("downloadedData");
});
        
/*
// Delete file
app.delete('/delete/:uid/:folder/:filename', async (req, res) => {
    try {
        const { uid, folder, filename } = req.params;
        const blobName = `${uid}/${folder}/${filename}`;
        
        const containerClient = blobServiceClient.getContainerClient(containerName);
        const blockBlobClient = containerClient.getBlockBlobClient(blobName);

        // Check if blob exists
        const exists = await blockBlobClient.exists();
        if (!exists) {
            return res.status(404).json({ error: 'File not found' });
        }

        await blockBlobClient.delete();
        
        res.json({ message: 'File deleted successfully' });
    } catch (error) {
        console.error('Delete error:', error);
        res.status(500).json({ error: 'Delete failed' });
    }
});*/
/*
// List files in a folder
app.get('/list/:uid/:folder', async (req, res) => {
    try {
        const { uid, folder } = req.params;
        const prefix = `${uid}/${folder}/`;
        
        const containerClient = blobServiceClient.getContainerClient(containerName);
        const files = [];

        for await (const blob of containerClient.listBlobsFlat({ prefix })) {
            files.push({
                name: blob.name,
                size: blob.properties.contentLength,
                lastModified: blob.properties.lastModified
            });
        }

        res.json({ files });
    } catch (error) {
        console.error('List error:', error);
        res.status(500).json({ error: 'Failed to list files' });
    }
});*/

app.listen(port, () => {
    console.log(`Server running on port ${port}`);
});