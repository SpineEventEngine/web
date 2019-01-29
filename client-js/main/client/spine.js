import {FirebaseClientFactory} from './firebase-client';
import {CustomClientFactory} from './client-factory';

/**
 * The main entry point of the `spine-web` JS library. Serves for initialization
 * of the `Client` instances to interact with Spine-based backend.
 *
 * To initialize a new instance of client that uses Firebase as a storage do the following:
 * ```
 *  import * as protobufs from './proto/index.js';
 *  import * as spineWeb from 'spine-web';
 *
 *  const firebaseApp = Firebase.initializeApp({...Firebase options});
 *
 *  // The backend client will receive updates of the current actor through this instance
 *  const actorProvider = new ActorProvider();
 *
 *  const client = spineWeb.initializeClient({
 *      protoIndexFiles: [protobufs],
 *      endpointUrl: 'http://example.appspot.com',
 *      firebaseDatabase: firebaseApp.database(),
 *      actorProvider: actorProvider
 *  });
 * ```
 *
 * To substitute a custom implementation of `Client` for tests do the following:
 * ```
 *  // An instance of class extending `spineWeb.Client`
 *  const mockClientImpl = new MockClient();
 *
 *  const mockClient = spineWeb.initializeClient({
 *      protoIndexFiles: [protobufs],
 *      implementation: mockClientImpl
 *  });
 * ```
 * Note, when using of custom `Client` implementation protobuf index files
 * registration is still required.
 *
 * @param {ClientOptions} options
 * @return {Client}
 */
export function initializeClient(options) {
  let clientFactory;

  if (!!options.firebaseDatabase) {
    clientFactory = FirebaseClientFactory;
  } else {
    clientFactory = CustomClientFactory;
  }

  return clientFactory.createClient(options);
}
