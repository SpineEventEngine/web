import KnownTypes from './known-types';
import TypeParsers from './parser/type-parsers';
import {Client} from './client';

/**
 * @typedef {Object} ClientOptions a type of object for initialization of Spine client
 *
 * @property {!Array<Object>} protoIndexFiles               the list of the `index.js` files generated by
 * {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
 * @property {?string} endpointUrl                          the optional URL of the Spine-based backend endpoint
 * @property {?firebase.database.Database} firebaseDatabase the optional Firebase Database that will be used to retrieve
 *                                                          data from
 * @property {?ActorProvider} actorProvider                 the optional provider of the user interacting with Spine
 * @property {?Client} implementation                       the optional custom implementation of `Client`
 */

/**
 * An abstract factory for creation of `Client` instances.
 *
 * Ensures that the `ClientOptions` contain list of the `index.js` files generated by
 * {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
 * and performs registration of types and parsers containing in these files.
 *
 * Creation of the concrete implementation of `Client` instances is delegated to inheritors.
 */
export class AbstractClientFactory {

  /**
   * Creates a new instance of `Client` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options client initialization options
   * @return {Client} a `Client` instance
   */
  static createClient(options) {
    this._ensureOptionsSufficient(options);
    this._registerTypes(...options.protoIndexFiles);
    return this._clientFor(options);
  }

  /**
   * Creates a new instance of `Client` implementation in accordance with given options.
   *
   * @param {!ClientOptions} options
   * @return {Client}
   * @protected
   */
  static _clientFor(options) {
    throw new Error('Not implemented in abstract base')
  }

  /**
   * Ensures whether options object is sufficient for client initialization.
   *
   * @param {!ClientOptions} options
   * @protected
   */
  static _ensureOptionsSufficient(options) {
    if (!options) {
      throw new Error('Unable to initialize client. The `ClientOptions` is undefined.');
    }

    const indexFiles = options.protoIndexFiles;
    if (!Array.isArray(indexFiles) || indexFiles.length === 0) {
      throw new Error('Only a non-empty array is allowed as ClientOptions.protoIndexFiles parameter.');
    }

    indexFiles.forEach(indexFile => {
      if (typeof indexFile !== 'object'
          || !(indexFile.types instanceof Map)
          || !(indexFile.parsers instanceof Map) ) {
        throw new Error('Unable to register Protobuf index files.' +
          ' Check the `ClientOptions.protoIndexFiles` contains files' +
          ' generated with "io.spine.tools:spine-proto-js-plugin".');
      }
    });
  }

  /**
   * Registers all Protobuf types provided by the specified modules.
   *
   * After the registration, the types can be used and parsed correctly.
   *
   * @param protoIndexFiles the index.js files generated by
   * {@link https://github.com/SpineEventEngine/base/tree/master/tools/proto-js-plugin the Protobuf plugin for JS}
   * @private
   */
  static _registerTypes(...protoIndexFiles) {
    for (let indexFile of protoIndexFiles) {
      for (let [typeUrl, type] of indexFile.types) {
        KnownTypes.register(type, typeUrl);
      }
      for (let [typeUrl, parserType] of indexFile.parsers) {
        TypeParsers.register(new parserType(), typeUrl);
      }
    }
  }
}

/**
 * An implementation of the `AbstractClientFactory` that returns a client instance
 * provided in `ClientOptions` parameter.
 */
export class CustomClientFactory extends AbstractClientFactory {

  /**
   * Returns a custom Client implementation provided in options. Expects that the
   * given options contain an implementation which extends `Client`.
   *
   * Can be used to provide mock implementations of `Client`.
   *
   * @param {ClientOptions} options
   * @return {Client} a custom `Client` implementation provided in options
   * @override
   */
  static _clientFor(options) {
    return options.implementation;
  }

  /**
   * @override
   */
  static _ensureOptionsSufficient(options) {
    super._ensureOptionsSufficient(options);
    const customClient = options.implementation;
    if (!customClient || !(customClient instanceof Client)) {
      throw new Error('Unable to initialize custom implementation.' +
        ' The `ClientOptions.implementation` should extend Client.');
    }
  }
}
